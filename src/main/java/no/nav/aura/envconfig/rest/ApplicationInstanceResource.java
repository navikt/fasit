package no.nav.aura.envconfig.rest;

import static no.nav.aura.envconfig.util.IpAddressResolver.resolveIpFrom;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.JsonValidator;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.ApplicationInstanceDO;
import no.nav.aura.envconfig.client.ClusterDO;
import no.nav.aura.envconfig.client.NodeDO;
import no.nav.aura.envconfig.client.PlatformTypeDO;
import no.nav.aura.envconfig.client.ResourceDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.ExposedServiceReference;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.infrastructure.Reference;
import no.nav.aura.envconfig.model.infrastructure.ResourceReference;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.util.Tuple;
import no.nav.aura.fasit.client.model.AppConfig;
import no.nav.aura.fasit.client.model.ExposedResource;
import no.nav.aura.fasit.client.model.MissingResource;
import no.nav.aura.fasit.client.model.RegisterApplicationInstancePayload;
import no.nav.aura.fasit.client.model.UsedResource;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.integration.FasitKafkaProducer;

@RestController
@RequestMapping(path = "/conf")
public class ApplicationInstanceResource {

    private static final Logger log = LoggerFactory.getLogger(ApplicationInstanceResource.class);
    
    private FasitKafkaProducer fasitKafkaProducer;
    private FasitRepository repository;
    private ApplicationInstanceRepository instanceRepository;


    protected ApplicationInstanceResource() {
        // For cglib and @transactional
    }

    @Inject
    public ApplicationInstanceResource(FasitRepository repository, ApplicationInstanceRepository instanceRepository, FasitKafkaProducer fasitKafkaProducer) {
        this.repository = repository;
        this.instanceRepository = instanceRepository;
        this.fasitKafkaProducer = fasitKafkaProducer;
    }

    @PostMapping(path = "/v1/applicationinstances", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registerApplicationInstance(@RequestBody String payload) {
    	Set<ValidationMessage> schemaValidatedJson = schemaValidateJsonString("/registerApplicationInstanceSchema.json", payload);
        if (!schemaValidatedJson.isEmpty()) {
            String errorMessages = schemaValidatedJson.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Input did not pass schema-validation: " + errorMessages);
        }
        
        ObjectMapper objectMapper = new ObjectMapper();
        try {
        	RegisterApplicationInstancePayload applicationInstancePayload = objectMapper.readValue(payload, RegisterApplicationInstancePayload.class);
        	ApplicationInstance applicationInstance = register(applicationInstancePayload);
        	return ResponseEntity.created(URI.create("/v1/applicationinstances/" + applicationInstance.getID())).body(applicationInstance.toString());
        } catch (JsonProcessingException e) {
			log.error("Unable to parse payload", e);
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error deserializing payload: " + e.getMessage(), e);
        }
    }

    @GetMapping(path = "/v1/environments/{environmentName}/applications/{applicationName}/full", produces = "application/json")
    public ApplicationInstanceDO getApplicationInstanceWithResources(
    		@PathVariable("environmentName") final String envName, 
    		@PathVariable("applicationName") final String appName,
    		UriComponentsBuilder uriBuilder) {
        Environment environment = findEnvironment(envName);
        ApplicationInstance instance = findApplicationInstance(appName, environment);
        ApplicationInstanceDO appDO = createApplicationDO(environment, instance, uriBuilder);

        appDO.setExposedServices(getResourceFromReference(instance.getExposedServices()));
        appDO.setUsedResources(getResourceFromReference(instance.getResourceReferences()));
        return appDO;
    }

    @GetMapping(path = "/v1/environments/{environmentName}/applications/{applicationName}", produces = "application/json")
    public ApplicationInstanceDO getApplicationInstance(
    		@PathVariable("environmentName") final String envName, 
    		@PathVariable("applicationName") final String appName, 
    		UriComponentsBuilder uriBuilder) {
        Environment environment = findEnvironment(envName);
        ApplicationInstance instance = findApplicationInstance(appName, environment);
        return createApplicationDO(environment, instance, uriBuilder);
    }

    protected ApplicationInstanceDO createApplicationDO(Environment environment, ApplicationInstance instance, UriComponentsBuilder uriBuilder) {
        ApplicationInstanceDO appDO = new ApplicationInstanceDO(instance.getApplication().getName(), environment.getName().toLowerCase(), uriBuilder);
        appDO.setDeployedBy(instance.getUpdatedBy());
        ZonedDateTime deployDate = instance.getDeployDate();
        if (deployDate != null) {
            appDO.setLastDeployment(deployDate);
        }
        appDO.setSelftestPagePath(instance.getSelftestPagePath());
        appDO.setAppConfigRef(uriBuilder.path("environments/{env}/applications/{appname}/appconfig").build(environment.getName(), instance.getApplication().getName()));
        appDO.setVersion(instance.getVersion());
        appDO.setCluster(createClusterDO(uriBuilder, environment, instance.getCluster()));
        appDO.setHttpsPort(instance.getHttpsPort());
        appDO.setLoadBalancerUrl(instance.getCluster().getLoadBalancerUrl());
        return appDO;
    }

    protected Set<ResourceDO> getResourceFromReference(Set<? extends Reference> references) {
    	return references.stream()
				.filter(filterNonExistingResources())
				.map(toResourceDO())
				.collect(Collectors.toSet());
    }

    private Function<Reference, ResourceDO> toResourceDO() {
        return input -> {
            Resource resource = input.getResource();
            return new ResourceDO(resource.getType().name(), resource.getScope().asDisplayString(), resource.getAlias(), resource.getProperties());
        };
    }

    private Predicate<Reference> filterNonExistingResources() {
    	return input -> input != null && input.getResource() != null;
    }

    private ClusterDO createClusterDO(UriComponentsBuilder uriBuilder, Environment environment, Cluster cluster) {
        if (cluster == null) {
            return null;
        }
        ClusterDO c = new ClusterDO();
        c.setName(cluster.getName());
        c.setEnvironmentClass(environment.getEnvClass().name());
        c.setEnvironmentName(environment.getName());
        c.setDomain(cluster.getDomain().getFqn());
        c.setLoadBalancerUrl(cluster.getLoadBalancerUrl());
        c.setApplications(applicationNames(cluster.getApplications()));
        Set<Node> nodes = cluster.getNodes();
        for (Node node : nodes) {
            NodeDO nodeDO = new NodeDO();
            nodeDO.setHostname(node.getHostname());
            nodeDO.setIpAddress(resolveIpFrom(node.getHostname()).orElse(null));
            nodeDO.setUsername(node.getUsername());
            URI ref = uriBuilder.path(SecretRestService.createPath(node.getPassword())).build().toUri();
            nodeDO.setPasswordRef(ref);
            nodeDO.setDomain(node.getDomain().getFqn());
            nodeDO.setPlatformType(PlatformTypeDO.valueOf(node.getPlatformType().name()));
            // TODO Deprecated 20.03.14 Remove this when we are certain no old versions of aura-maven-plugin are using this
            nodeDO.setHttpsPort(node.getPlatformType().getBaseHttpsPort());
            c.addNode(nodeDO);
        }
        return c;
    }

    private List<String> applicationNames(Collection<Application> applications) {
        return applications.stream()
                .map(input -> input.getName())
                .collect(Collectors.toList());
    }

    protected ApplicationInstance findApplicationInstance(final String appName, Environment environment) {
        ApplicationInstance appInstance = environment.findApplicationByName(appName);
        if (appInstance == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application " + appName + " is not defined in environment " + environment.getName());
        }
        return appInstance;
    }

    protected Environment findEnvironment(String envName) {
        Environment environment = repository.findEnvironmentBy(envName.toLowerCase());
        if (environment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Environment " + envName + " not found");
        }
        return environment;
    }

    @Transactional
    public ApplicationInstance register(RegisterApplicationInstancePayload payload) {

        final String applicationName = payload.getApplication();
        final String version = payload.getVersion();
        final String environmentName = payload.getEnvironment();
        final List<String> nodes = payload.getNodes();

        verifyInfrastructure(applicationName, environmentName, nodes);

        final Set<UsedResource> usedResources = payload.getUsedResources();
        verifyUsedResources(usedResources);

        final List<ExposedResource> exposedResources = payload.getExposedResources();
        verifyExposedResources(exposedResources);

        final List<MissingResource> missingResources = payload.getMissingResources();
        verifyMissingResources(missingResources);

        ApplicationInstance applicationInstance = instanceRepository.findInstanceOfApplicationInEnvironment(applicationName, environmentName);

        applicationInstance.setResourceReferences(createResourceReferences(usedResources, missingResources, exposedResources));
        applicationInstance.setExposedServices(createExposedResources(applicationName, environmentName, exposedResources));

        applicationInstance.setDeployDate(ZonedDateTime.now());
        applicationInstance.setVersion(version);
        applicationInstance.setSelftestPagePath(payload.getSelftest());

        final AppConfig appConfig = payload.getAppConfig();

        if (appConfig != null) {
            applicationInstance.setAppconfigXml(StringEscapeUtils.unescapeEcmaScript(appConfig.getContent()));
        }

        applicationInstance = repository.store(applicationInstance);

        fasitKafkaProducer.publishDeploymentEvent(applicationInstance, findEnvironment(environmentName));

        log.debug("Registered new application instance of application {} with version {} to environment {}", applicationName, version, environmentName);
        return applicationInstance;

    }

    protected static Set<ValidationMessage> schemaValidateJsonString(String schemaPath, String string) {
        try {
        	
        	InputStream schemaStream = JsonValidator.class.getResourceAsStream(schemaPath);
	   		 if (schemaStream == null) {
	             throw new IllegalArgumentException("Schema not found at path: " + schemaPath);
			 }
    		JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V7);
	    	JsonSchema schema = jsonSchemaFactory.getSchema(schemaStream);
	    	
	        ObjectMapper objectMapper = new ObjectMapper();

        	JsonNode jsonNode = objectMapper.readTree(validateJson(string));

        	return schema.validate(jsonNode);
	        	
//            ProcessingReport validation = validator.validate(JsonLoader.fromResource(schemaPath), JsonLoader.fromString(validateJson(string)));
//            
//            if (!validation.isSuccess()) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Input did not pass schema-validation. " + validation.toString());
//            }
        } 
        catch (Exception e) {
            throw new RuntimeException("UGHHH, internal error. Please stay calm.", e);
        }
//        catch (ProcessingException e) {
//            log.error("Invalid JSON Schema", e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "UGHHH, internal error. Please stay calm.");
//        } catch (IOException e) {
//            log.error("Unable get JSON Schema", e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "UGHHH, internal error. Please stay calm.");
//        }
//
//        return string;
    }

    protected static String validateJson(final String string) {
        try {
            new ObjectMapper().readValue(string, Map.class);
            return string;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to understand input payload. Is your JSON valid? Reason: " + e.getMessage(), e);
        }
    }

    private void verifyInfrastructure(String applicationName, String environmentName, List<String> nodes) {
        verifyApplicationExists(applicationName);
        verifyEnvironmentExists(environmentName);
        verifyNodesExist(nodes);
        verifyApplicationIsDefinedInEnvironment(applicationName, environmentName);
    }

    protected void verifyApplicationExists(String applicationName) {
        if (null == repository.findApplicationByName(applicationName)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application " + applicationName + " was not found in Fasit");
        }
    }

    public void verifyEnvironmentExists(String environmentName) {
        if (null == repository.findEnvironmentBy(environmentName)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Environment " + environmentName + " was not found in Fasit");
        }
    }

    public void verifyNodesExist(List<String> nodes) {
        for (String hostname : nodes) {
            log.warn("Verifying if node exists: {}", hostname);
            if (null == repository.findNodeBy(hostname)) {
                log.warn("Node does not exists: {}", hostname);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node with hostname " + hostname + " was not found in Fasit");
            }
        }
    }

    protected void verifyApplicationIsDefinedInEnvironment(String applicationName, String environmentName) {
        if (null == instanceRepository.findInstanceOfApplicationInEnvironment(applicationName, environmentName)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application " + applicationName + " has not been mapped to a cluster in environment " + environmentName);
        }
    }

    protected void verifyUsedResources(Collection<UsedResource> usedResources) {
        for (UsedResource usedResource : usedResources) {
            try {
                repository.getRevision(Resource.class, usedResource.getId(), usedResource.getRevision());
            } catch (Exception e) {
                if (e instanceof RevisionDoesNotExistException) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find the used resource with id " + usedResource.getId() + " and revision " + usedResource.getRevision() + ". Message: " + e.getMessage(), e);
                } else {
                    log.error("Unable to verify used resource", e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify used resource, something bad happened internally :(");
                }
            }
        }
    }

    protected void verifyExposedResources(Collection<ExposedResource> exposedResources) {
        for (ExposedResource exposedResource : exposedResources) {
            final String typeName = exposedResource.getType();

            if (!ResourceType.resourceTypeWithNameExists(typeName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exposed resource " + exposedResource + " has an unknown resource type " + typeName + ".\n Valid types are: "
						+ String.join(", ", ResourceType.getAllResourceTypeNames()));
            }
            ResourceType resourceType = ResourceType.getResourceTypeFromName(exposedResource.getType());
            verifyOnlyExistingFieldsAreProvided(exposedResource);
            if (isExposedInternally(resourceType)) {
                verifyMandatoryFieldsAreProvided(exposedResource);
            } else {
                verifyResourceExists(exposedResource);
            }
        }
    }

    private void verifyResourceExists(ExposedResource exposedResource) {
        ResourceType resourceType = ResourceType.getResourceTypeFromName(exposedResource.getType());
        if (exposedResource.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exposed resource " + exposedResource + " is missing required property id");
        }
        Resource found = repository.getById(Resource.class, exposedResource.getId());
        if (found == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exposed resource " + exposedResource + " is not found in Fasit with id " + exposedResource.getId());
        }
        if (found.getType() != resourceType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exposed resource " + exposedResource + " is stored in Fasit with other resourceType " + found.getType());
        }
        if (!found.getAlias().equalsIgnoreCase(exposedResource.getAlias())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exposed resource " + exposedResource + " is stored in Fasit with other alias " + found.getAlias());
        }

    }

    private void verifyMandatoryFieldsAreProvided(ExposedResource exposedResource) {
        ResourceType resourceType = ResourceType.getResourceTypeFromName(exposedResource.getType());

        List<String> mandatoryFields = ResourceType.getMandatoryFieldsFor(resourceType);
        for (String mandatoryField : mandatoryFields) {
            if (!exposedResource.getProperties().containsKey(mandatoryField)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mandatory field " + mandatoryField + " was not found for exposed resource " + exposedResource + ". \n Mandatory fields are: "
						+ String.join(", ", mandatoryFields));
            }
        }
    }

    private void verifyOnlyExistingFieldsAreProvided(ExposedResource exposedResource) {
        ResourceType resourceType = ResourceType.getResourceTypeFromName(exposedResource.getType());
        List<String> allFields = ResourceType.getAllFieldsFor(resourceType);

        for (String property : exposedResource.getProperties().keySet()) {
            if (!allFields.contains(property)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided property field " + property + " is not a valid field for exposed resource " + exposedResource + ". \n Valid fields are: "
						+ String.join(", ",allFields));
            }
        }
    }

    protected static void verifyMissingResources(List<MissingResource> missingResources) {
        for (MissingResource missingResource : missingResources) {
            String typeName = missingResource.getType().name();

            if (!ResourceType.resourceTypeWithNameExists(typeName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing resource with alias " + missingResource.getAlias() + " has an unknown resource type " + typeName + ".\n Valid types are: "
						+ String.join(", ", ResourceType.getAllResourceTypeNames()));
            }
        }
    }

    private Set<ExposedServiceReference> createExposedResources(String applicationName, String environmentName, Collection<ExposedResource> exposedResources) {

        final ApplicationInstance existingAppInstance = instanceRepository.findInstanceOfApplicationInEnvironment(applicationName, environmentName);
        Set<ExposedServiceReference> newExposedServices = new HashSet<>();

        for (ExposedResource exposedResource : exposedResources) {
            log.debug("Added exposed resource " + exposedResource.getId() + " " + exposedResource.getAlias());
            ExposedServiceReference serviceReference = findExistingExposedResources(exposedResource, existingAppInstance.getExposedServices());

            if (serviceReference == null) {
                log.debug("No service reference found ");
                ResourceType resourceType = ResourceType.getResourceTypeFromName(exposedResource.getType());
                if (isExposedInternally(resourceType)) {
                    log.debug("Resource is exposed internally, creating new");
                    serviceReference = createNewServiceReference(exposedResource, environmentName);
                } else {
                    Resource storedResource = repository.getById(Resource.class, exposedResource.getId());
                    log.debug("Getting by ID " + exposedResource.getId() + " (from payload) " + storedResource.getID() + " (stored)");
                    serviceReference = new ExposedServiceReference(storedResource, null);
                }
            }

            log.debug("Service reference " + serviceReference.getName() + " " + serviceReference.getID());
            updateResourceIfChanged(exposedResource, serviceReference, environmentName);

            List<Tuple<Long, RevisionType>> history = repository.getRevisionsFor(Resource.class, serviceReference.getResource().getID());
            Long revision = history.isEmpty() ? null : history.get(0).fst;
            serviceReference.setRevision(revision);
            newExposedServices.add(serviceReference);

            checkResolvedFutureResourceReferences(serviceReference.getResource());
        }

        Set<ExposedServiceReference> toBeDeleted = new HashSet<>(existingAppInstance.getExposedServices());
        toBeDeleted.removeAll(newExposedServices);

        for (ExposedServiceReference exposedServiceReference : toBeDeleted) {
            log.debug("Removing no longer exposed service: {}", exposedServiceReference);
            repository.delete(exposedServiceReference);
        }

        return newExposedServices;
    }

    private boolean isExposedInternally(ResourceType type) {
        return !ResourceType.externalExposedResourceTypes.contains(type);
    }

    private ExposedServiceReference findExistingExposedResources(ExposedResource exposedResource, Set<ExposedServiceReference> exposedServices) {
        for (ExposedServiceReference exposedServiceReference : exposedServices) {
            if (exposedServiceReference.getResourceAlias().equals(exposedResource.getAlias())) {
                return exposedServiceReference;
            }
        }
        return null;
    }

    private ExposedServiceReference updateResourceIfChanged(ExposedResource exposedResource, ExposedServiceReference serviceReference, String environmentName) {
        boolean changed = false;
        Resource resource = serviceReference.getResource();
        if (exposedResource.getAccessAdGroups() != null && !exposedResource.getAccessAdGroups().equals(resource.getAccessControl().getAdGroups())) {
            resource.getAccessControl().setAdGroups(exposedResource.getAccessAdGroups());
            changed = true;
        }

        Scope expectedScope = createScopeForResource(exposedResource, repository.findEnvironmentBy(environmentName));
        if (!resource.getScope().equals(expectedScope)) {
            resource.setScope(expectedScope);
            changed = true;
        }

        Map<String, String> props = exposedResource.getProperties();
        for (Map.Entry<String, String> property : props.entrySet()) {
            String key = property.getKey();
            String value = property.getValue();
            if (resource.getSecrets().containsKey(key)) {
                if (!value.equals(resource.getSecrets().get(key).getClearTextString())) {
                    resource.putSecretAndValidate(key, value);
                    changed = true;
                }
            } else {
                if (!value.equals(resource.getProperties().get(key))) {
                    resource.putPropertyAndValidate(key, value);
                    changed = true;
                }
            }
        }
        if (changed) {
            serviceReference.setResource(repository.store(resource));
            log.debug("Updated resource for exposed service: {}", resource);
        }
        return serviceReference;

    }

    private ExposedServiceReference createNewServiceReference(ExposedResource exposedResource, String environmentName) {

        ResourceType resourceType = ResourceType.getResourceTypeFromName(exposedResource.getType());
        String alias = exposedResource.getAlias();
        Scope scope = createScopeForResource(exposedResource, repository.findEnvironmentBy(environmentName));
        List<Resource> storedResources = repository.findResourcesByExactAlias(scope, resourceType, alias);
        if (!storedResources.isEmpty()) {
            for (Resource foundResource : storedResources) {
                log.debug("Checking scope of found resource: {}", foundResource);
                if (scope.equals(foundResource.getScope())) {
                    log.error("Found exposed resource {}:{} in scope not exposed by this application", foundResource);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Found exposed resource %s:%s in scope %s not exposed by this application", resourceType, alias, scope));
                }
            }
        }
        ExposedServiceReference serviceReference = new ExposedServiceReference(new Resource(alias, resourceType, scope), null);
        log.debug("Created new service reference: {}", serviceReference);
        return serviceReference;

    }

    protected static Scope createScopeForResource(ExposedResource exposedResource, Environment environment) {
        Scope scope = new Scope(environment);
        String domain = exposedResource.getDomain();

        if (domain != null) {
            scope = scope.domain(Domain.fromFqdn(domain));
        }

        return scope;
    }

    private void checkResolvedFutureResourceReferences(Resource resource) {
        Collection<ResourceReference> futureReferences = repository.findFutureResourceReferencesBy(resource.getAlias(), resource.getType());
        for (ResourceReference future : futureReferences) {
            ApplicationInstance futureApplication = repository.getApplicationInstanceBy(future);
            Environment futureEnvironment = repository.getEnvironmentBy(futureApplication);
            Scope futureScope = new Scope(futureEnvironment).domain(futureApplication.getCluster().getDomain());

            if (futureScope.isSubsetOf(resource.getScope())) {
                future.setResource(resource);
                log.debug("Updating future reference {} for {} in {} ", resource.getName(), futureApplication.getName(), futureEnvironment.getName());
                repository.store(future);
            }
        }
    }

    private Set<ResourceReference> createResourceReferences(Collection<UsedResource> usedResources, Collection<MissingResource> missingResources, List<ExposedResource> exposedResources) {
        Set<ResourceReference> resourceReferences = new HashSet<>();
        for (UsedResource usedResource : usedResources) {
            Resource resource = repository.getById(Resource.class, usedResource.getId());
            if (!isExposed(resource, exposedResources)) {
                ResourceReference resourceReference = new ResourceReference(resource, usedResource.getRevision());
                resourceReferences.add(resourceReference);
            } else {
                log.debug("Resource {} will not be registered as used since it is exposed by this application", resource);
            }
        }
        for (MissingResource missingResource : missingResources) {
            String alias = (String) missingResource.getAlias();
            ResourceType resourceType = ResourceType.getResourceTypeFromName(missingResource.getType().name());
            ResourceReference futureResource = ResourceReference.future(alias, resourceType);
            resourceReferences.add(futureResource);
        }

        return resourceReferences;
    }

    private boolean isExposed(final Resource resource, final List<ExposedResource> exposedResources) {
        for (ExposedResource exposedResource : exposedResources) {
            if (exposedResource.getAlias().equals(resource.getAlias()) && resource.getType().name().equals(exposedResource.getType())) {
                return true;
            }
        }
        return false;
    }

}
