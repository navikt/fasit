package no.nav.aura.envconfig.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.gson.Gson;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.*;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.util.Tuple;
import no.nav.aura.fasit.client.model.*;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.integration.FasitKafkaProducer;
import no.nav.aura.integration.VeraRestClient;
import no.nav.aura.sensu.SensuClient;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static java.lang.String.format;
import static no.nav.aura.envconfig.util.IpAddressResolver.resolveIpFrom;

@Component
@Path("/conf/")
public class ApplicationInstanceResource {

    private static final Logger log = LoggerFactory.getLogger(ApplicationInstanceResource.class);
    private VeraRestClient vera;
    private FasitKafkaProducer fasitKafkaProducer;
    private FasitRepository repository;
    private ApplicationInstanceRepository instanceRepository;
    private SensuClient sensuClient;

    @Context
    private UriInfo uriInfo;

    protected ApplicationInstanceResource() {
        // For cglib and @transactional
    }

    @Inject
    public ApplicationInstanceResource(FasitRepository repository, ApplicationInstanceRepository instanceRepository, SensuClient sensuClient, FasitKafkaProducer fasitKafkaProducer, VeraRestClient vera) {
        this.repository = repository;
        this.instanceRepository = instanceRepository;
        this.sensuClient = sensuClient;
        this.fasitKafkaProducer = fasitKafkaProducer;
        this.vera = vera;
    }

    @POST
    @Consumes(value = "application/json")
    @Path("/v1/applicationinstances")
    public Response registerApplicationInstance(final String payload) {
        String schemaValidatedJson = schemaValidateJsonString("/registerApplicationInstanceSchema.json", payload);
        Gson gson = new Gson();
        RegisterApplicationInstancePayload applicationInstancePayload = gson.fromJson(schemaValidatedJson, RegisterApplicationInstancePayload.class);
        ApplicationInstance applicationInstance = register(applicationInstancePayload);
        sensuClient.sendEvent("fasit.jsonSchemaAppInstanceService.post", Collections.emptyMap(),
                ImmutableMap.of(
                        "appName", applicationInstancePayload.getApplication(),
                        "envName", applicationInstancePayload.getEnvironment()));
        return Response.created(URI.create("/v1/applicationinstances/" + applicationInstance.getID())).entity(applicationInstance.toString()).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/environments/{environmentName}/applications/{applicationName}/full")
    public ApplicationInstanceDO getApplicationInstanceWithResources(@PathParam("environmentName") final String envName, @PathParam("applicationName") final String appName) {
        sensuClient.sendEvent("fasit.jsonSchemaAppInstanceService.get", Collections.emptyMap(),
                ImmutableMap.of(
                        "appName", appName,
                        "envName", envName));
        Environment environment = findEnvironment(envName);
        ApplicationInstance instance = findApplicationInstance(appName, environment);
        ApplicationInstanceDO appDO = createApplicationDO(environment, instance);

        appDO.setExposedServices(getResourceFromReference(instance.getExposedServices()));
        appDO.setUsedResources(getResourceFromReference(instance.getResourceReferences()));
        return appDO;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/v1/environments/{environmentName}/applications/{applicationName}")
    public ApplicationInstanceDO getApplicationInstance(@PathParam("environmentName") final String envName, @PathParam("applicationName") final String appName) {
        Environment environment = findEnvironment(envName);
        ApplicationInstance instance = findApplicationInstance(appName, environment);
        return createApplicationDO(environment, instance);
    }

    protected ApplicationInstanceDO createApplicationDO(Environment environment, ApplicationInstance instance) {
        ApplicationInstanceDO appDO = new ApplicationInstanceDO(instance.getApplication().getName(), environment.getName().toLowerCase(), uriInfo.getBaseUriBuilder());
        appDO.setDeployedBy(instance.getUpdatedBy());
        DateTime deployDate = instance.getDeployDate();
        if (deployDate != null) {
            appDO.setLastDeployment(deployDate.toDate());
        }
        appDO.setSelftestPagePath(instance.getSelftestPagePath());
        appDO.setAppConfigRef(uriInfo.getBaseUriBuilder().path("environments/{env}/applications/{appname}/appconfig").build(environment.getName(), instance.getApplication().getName()));
        appDO.setVersion(instance.getVersion());
        appDO.setCluster(createClusterDO(uriInfo, environment, instance.getCluster()));
        appDO.setHttpsPort(instance.getHttpsPort());
        appDO.setLoadBalancerUrl(instance.getCluster().getLoadBalancerUrl());
        return appDO;
    }

    protected Set<ResourceDO> getResourceFromReference(Set<? extends Reference> references) {
        return FluentIterable
                .from(references)
                .filter(filterNonExistingResources())
                .transform(toResourceDO())
                .toSet();
    }

    private Function<Reference, ResourceDO> toResourceDO() {
        return new Function<Reference, ResourceDO>() {
            public ResourceDO apply(Reference input) {
                Resource resource = input.getResource();
                return new ResourceDO(resource.getType().name(), resource.getScope().asDisplayString(), resource.getAlias(), resource.getProperties());
            }
        };
    }

    private Predicate<Reference> filterNonExistingResources() {
        return new Predicate<Reference>() {
            @Override
            public boolean apply(Reference input) {
                return input != null && input.getResource() != null;
            }
        };
    }

    private ClusterDO createClusterDO(UriInfo uriInfo, Environment environment, Cluster cluster) {
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
            nodeDO.setIpAddress(resolveIpFrom(node.getHostname()).orNull());
            nodeDO.setUsername(node.getUsername());
            URI ref = UriBuilder.fromUri(uriInfo.getBaseUri()).path(SecretRestService.createPath(node.getPassword())).build();
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
        return FluentIterable.from(applications).transform(new Function<Application, String>() {
            public String apply(Application input) {
                return input.getName();
            }
        }).toList();
    }

    protected ApplicationInstance findApplicationInstance(final String appName, Environment environment) {
        ApplicationInstance appInstance = environment.findApplicationByName(appName);
        if (appInstance == null) {
            throw new NotFoundException("Application " + appName + " is not defined in environment " + environment.getName());
        }
        return appInstance;
    }

    protected Environment findEnvironment(String envName) {
        Environment environment = repository.findEnvironmentBy(envName.toLowerCase());
        if (environment == null) {
            throw new NotFoundException("Environment " + envName + " not found");
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

        applicationInstance.setDeployDate(DateTime.now());
        applicationInstance.setVersion(version);
        applicationInstance.setSelftestPagePath(payload.getSelftest());

        final AppConfig appConfig = payload.getAppConfig();

        if (appConfig != null) {
            applicationInstance.setAppconfigXml(StringEscapeUtils.unescapeEcmaScript(appConfig.getContent()));
        }

        applicationInstance = repository.store(applicationInstance);

        sensuClient.sendEvent("fasit.deployments",
                ImmutableMap.of("deployedApplication", applicationName,
                        "targetEnvironment", environmentName),
                ImmutableMap.of("version", version));

        vera.notifyVeraOfDeployment(applicationInstance, findEnvironment(environmentName));

        log.debug(format("Registered new application instance of application %s with version %s to environment %s", applicationName, version, environmentName));
        return applicationInstance;

    }

    protected static String schemaValidateJsonString(String schemaPath, String string) {
        JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();

        try {
            ProcessingReport validation = validator.validate(JsonLoader.fromResource(schemaPath), JsonLoader.fromString(validateJson(string)));
            if (!validation.isSuccess()) {
                throw new BadRequestException("Input did not pass schema-validation. " + validation.toString());
            }
        } catch (ProcessingException e) {
            log.error("Invalid JSON Schema", e);
            throw new InternalServerErrorException("UGHHH, internal error. Please stay calm.");
        } catch (IOException e) {
            log.error("Unable get JSON Schema", e);
            throw new InternalServerErrorException("UGHHH, internal error. Please stay calm.");
        }

        return string;
    }

    protected static String validateJson(final String string) {
        try {
            new ObjectMapper().readValue(string, Map.class);
            return string;
        } catch (IOException e) {
            throw new BadRequestException("Unable to understand input payload. Is your JSON valid? Reason: " + e.getMessage(), e);
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
            throw new NotFoundException("Application " + applicationName + " was not found in Fasit");
        }
    }

    public void verifyEnvironmentExists(String environmentName) {
        if (null == repository.findEnvironmentBy(environmentName)) {
            throw new NotFoundException("Environment " + environmentName + " was not found in Fasit");
        }
    }

    public void verifyNodesExist(List<String> nodes) {
        for (String hostname : nodes) {
            if (null == repository.findNodeBy(hostname)) {
                throw new NotFoundException("Node with hostname " + hostname + " was not found in Fasit");
            }
        }
    }

    protected void verifyApplicationIsDefinedInEnvironment(String applicationName, String environmentName) {
        if (null == instanceRepository.findInstanceOfApplicationInEnvironment(applicationName, environmentName)) {
            throw new NotFoundException("Application " + applicationName + " has not been mapped to a cluster in environment " + environmentName);
        }
    }

    protected void verifyUsedResources(Collection<UsedResource> usedResources) {
        for (UsedResource usedResource : usedResources) {
            try {
                repository.getRevision(Resource.class, usedResource.getId(), usedResource.getRevision());
            } catch (Exception e) {
                if (e instanceof RevisionDoesNotExistException) {
                    throw new NotFoundException("Unable to find the used resource with id " + usedResource.getId() + " and revision " + usedResource.getRevision() + ". Message: " + e.getMessage(), e);
                } else {
                    log.error("Unable to verify used resource", e);
                    throw new InternalServerErrorException("Unable to verify used resource, something bad happened internally :(");
                }
            }
        }
    }

    protected void verifyExposedResources(Collection<ExposedResource> exposedResources) {
        for (ExposedResource exposedResource : exposedResources) {
            final String typeName = exposedResource.getType();

            if (!ResourceType.resourceTypeWithNameExists(typeName)) {
                throw new BadRequestException("Exposed resource " + exposedResource + " has an unknown resource type " + typeName + ".\n Valid types are: "
                        + Joiner.on(", ").join(ResourceType.getAllResourceTypeNames()));
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
            throw new BadRequestException("Exposed resource " + exposedResource + " is missing required property id");
        }
        Resource found = repository.getById(Resource.class, exposedResource.getId());
        if (found == null) {
            throw new BadRequestException("Exposed resource " + exposedResource + " is not found in Fasit with id " + exposedResource.getId());
        }
        if (found.getType() != resourceType) {
            throw new BadRequestException("Exposed resource " + exposedResource + " is stored in Fasit with other resourceType " + found.getType());
        }
        if (!found.getAlias().equalsIgnoreCase(exposedResource.getAlias())) {
            throw new BadRequestException("Exposed resource " + exposedResource + " is stored in Fasit with other alias " + found.getAlias());
        }

    }

    private void verifyMandatoryFieldsAreProvided(ExposedResource exposedResource) {
        ResourceType resourceType = ResourceType.getResourceTypeFromName(exposedResource.getType());

        List<String> mandatoryFields = ResourceType.getMandatoryFieldsFor(resourceType);
        for (String mandatoryField : mandatoryFields) {
            if (!exposedResource.getProperties().containsKey(mandatoryField)) {
                throw new BadRequestException("Mandatory field " + mandatoryField + " was not found for exposed resource " + exposedResource + ". \n Mandatory fields are: "
                        + Joiner.on(", ").join(mandatoryFields));
            }
        }
    }

    private void verifyOnlyExistingFieldsAreProvided(ExposedResource exposedResource) {
        ResourceType resourceType = ResourceType.getResourceTypeFromName(exposedResource.getType());
        List<String> allFields = ResourceType.getAllFieldsFor(resourceType);

        for (String property : exposedResource.getProperties().keySet()) {
            if (!allFields.contains(property)) {
                throw new BadRequestException("Provided property field " + property + " is not a valid field for exposed resource " + exposedResource + ". \n Valid fields are: "
                        + Joiner.on(", ").join(allFields));
            }
        }
    }

    protected static void verifyMissingResources(List<MissingResource> missingResources) {
        for (MissingResource missingResource : missingResources) {
            String typeName = missingResource.getType().name();

            if (!ResourceType.resourceTypeWithNameExists(typeName)) {
                throw new BadRequestException("Missing resource with alias " + missingResource.getAlias() + " has an unknown resource type " + typeName + ".\n Valid types are: "
                        + Joiner.on(", ").join(ResourceType.getAllResourceTypeNames()));
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

        SetView<ExposedServiceReference> toBeDeleted = Sets.difference(existingAppInstance.getExposedServices(), newExposedServices);
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
                    throw new BadRequestException(String.format("Found exposed resource %s:%s in scope %s not exposed by this application", resourceType, alias, scope));
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
