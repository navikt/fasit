package no.nav.aura.envconfig.rest;

import static no.nav.aura.envconfig.rest.util.Converters.toEnumOrNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.RevisionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.ApplicationInstanceDO;
import no.nav.aura.envconfig.client.DomainDO;
import no.nav.aura.envconfig.client.LifeCycleStatusDO;
import no.nav.aura.envconfig.client.ResourceTypeDO;
import no.nav.aura.envconfig.client.rest.PropertyElement;
import no.nav.aura.envconfig.client.rest.PropertyElement.Type;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.envconfig.client.rest.ResourceElementList;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.FileEntity;
import no.nav.aura.envconfig.model.resource.PropertyField;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.util.Tuple;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;

/**
 * Api for å h�ndtere ressurser i envconfig
 */
@RestController
@RequestMapping("/conf/resources")
public class ResourcesRestService {

    private FasitRepository repo;

    private Logger log = LoggerFactory.getLogger(ResourcesRestService.class);
    private ApplicationInstanceRepository applicationInstanceRepository;

    public ResourcesRestService() {
    }

    @Inject
    public ResourcesRestService(FasitRepository repo, ApplicationInstanceRepository applicationInstanceRepository) {
        this.repo = repo;
        this.applicationInstanceRepository = applicationInstanceRepository;
    }

    /**
     * Finner den ressursen som passer best med søkekriteriene ut fra en vekting. Alle søkeparametere er påkrevd
     * 
     * @param envClass
     *            miljøklasse, u,t,q eller p
     * @param envName
     *            miljønavn feks t8
     * @param domain
     *            fqdn til domenet
     * @param application
     *            navnet på applikasjonen
     * @param type
     *            {@linkplain ResourceType} ressurstype
     * @param alias
     *            navn/alias for ressursen i envconfig
     * @return ressursen
     * 
     * @HTTP 400 ved noe feil i inputparametere
     * @HTTP 404 Hvis det ikke er noen treff
     */
    @GetMapping(path = "/bestmatch", produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResourceElement getBestMatchingResource(
            @RequestParam(name = "envClass", required = false) String envClass,
            @RequestParam(name = "envName", required = false) String envName, 
            @RequestParam(name = "domain", required = false) String domain,
            @RequestParam(name = "app", required = false) String application, 
            @RequestParam(name = "type", required = false) ResourceTypeDO type, 
            @RequestParam(name = "alias", required = false) String alias) {

        Scope scope = getSearchScope(envClass, envName, domain, application);

        if (scope.getApplication() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad or missing required parameter 'app'");
        }
        if (scope.getDomain() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad or missing required parameter 'domain'");
        }

        if (scope.getEnvironmentName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad or missing required parameter 'envName'");
        }

        ResourceType resourceType = toEnumOrNull(ResourceType.class, enumNameOrNull(type));
        if (resourceType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad or missing required parameter 'type'");
        }

        if (StringUtils.isEmpty(alias)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter 'alias'");
        }

        ResourceElementList resources = findResources(envClass, envName, domain, application, type, alias, true, false);
        if (resources.size() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Found no active resources with alias " + alias + " of type " + resourceType + " in scope " + scope);
        }

        if (resources.size() != 1) {
            throw new RuntimeException("Found " + resources.size() + " resources with alias " + alias + " of type " + resourceType + " in scope " + scope + ". Expected only one");
        }

        return resources.getResourceElements().get(0);
    }

    /**
     * Finner alle ressurser som matcher alle søkekriterier. Hvis det er flere ressurser som med samme alias av samme type
     * forsøkes det å finne den som matcher best ved å bruke vekt av scope.
     * 
     * @param envClass
     *            miljøklasse, u,y,q eller p
     * @param envName
     *            miljønavn feks t8
     * @param domain
     *            fqdn til domenet
     * @param application
     *            navnet på applikasjonen
     * @param type
     *            {@linkplain ResourceType} ressurstype
     * @param alias
     *            navn/alias for ressursen i envconfig
     * @param bestmatch
     *            Settes til true om det skal gjøres noe bestmatch filtrering. Default brukes det ikke bestmatch
     * @param usage
     *            Settes til true for å se hvilke applikasjonsinstanser som faktisk bruker ressursen
     * @return liste med ressurser
     * 
     * @HTTP 400 ved noe feil i inputparametere
     */

    @GetMapping(produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResourceElementList findResources(
            @RequestParam(required = false) String envClass, 
            @RequestParam(required = false) String envName, 
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String app, 
            @RequestParam(required = false) ResourceTypeDO type, 
            @RequestParam(required = false) String alias, 
            @RequestParam(required = false) Boolean bestmatch,
            @RequestParam(required = false, defaultValue = "false") Boolean usage) {

        final Scope scope = getSearchScope(envClass, envName, domain, app);

        final Boolean showUsedInApplications = usage;

        ResourceType resourceType = toEnumOrNull(ResourceType.class, enumNameOrNull(type));
        Collection<Resource> allResourcesWithAlias = repo.findResourcesByExactAlias(scope, resourceType, alias);
        Collection<Resource> activeResources = allResourcesWithAlias;//removeStoppedResouces(allResourcesWithAlias);

        if (activeResources.isEmpty()) {
            log.debug("Found no resources with alias:{} of type:{} in {}", alias, resourceType, scope);
            return new ResourceElementList();
        }

        if (bestmatch != null && bestmatch) {
            activeResources = filterResultsForBestMatch(scope, activeResources);
        }

        log.info("Found {} resources with alias:{} of type:{} in {} , bestmatch: {}", activeResources.size(), alias, resourceType, scope, bestmatch);

        List<ResourceElement> elements = activeResources.stream()
                .map(resource -> createResourceElement(resource, showUsedInApplications))
                .collect(Collectors.toList());
        
        return new ResourceElementList(elements);
    }

    private Collection<Resource> filterResultsForBestMatch(final Scope scope, Collection<Resource> activeResources) {
        // create a multimap with all resources of a type and its name
        Map<String, List<Resource>> resourcesMap = new HashMap<>();

        for (Resource resource : activeResources) {
            String key = resource.getAlias().toLowerCase() + resource.getType();
            resourcesMap.computeIfAbsent(key, k -> new ArrayList<>()).add(resource);
        }
        // Filter out best matching resources
        Map<String, Resource> bestMatchMap = new HashMap<>();
        for (Map.Entry<String, List<Resource>> entry : resourcesMap.entrySet()) {
            bestMatchMap.put(entry.getKey(), scope.singleBestMatch(entry.getValue()));
        }

        return bestMatchMap.values();
    }


    /**
     * Legger til en ressurs i fasit
     * 
     * @param uriInfo
     *            uriInfo
     * @param input
     *            multipart form der hver property i ressursen har sin egen part
     * 
     * @HTTP 400 ved feil eller manglende properties
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @Transactional
    public ResourceElement createResource(MultipartHttpServletRequest request) {
//        Map<String, List<InputPart>> multipartMap = request.getFormDataMap();
        ResourceTypeDO resourceType = toEnumOrNull(ResourceTypeDO.class, getSingleAsString(request, "type"));
        return createResource(resourceType, request);
    }

    /**
     * Legger til en ressurs i envconfig
     * 
     * @param type
     *            ressurstypen
     * @param input
     *            multipart form der hver property i ressursen har sin egen part
     * 
     * @HTTP 400 ved feil eller manglende properties
     * @deprecated -use createResource
     */
    @PutMapping(path = "/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    @Deprecated
    @Transactional
    public ResourceElement addResource(@PathVariable(name = "type") ResourceTypeDO type, MultipartHttpServletRequest request) {
        return createResource(type, request);
    }

    private ResourceElement createResource(ResourceTypeDO type, MultipartHttpServletRequest request) {
        ResourceType resourceType = toEnumOrNull(ResourceType.class, enumNameOrNull(type));
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter type. Legal values is: " + Arrays.asList(ResourceType.values()));
        }

        Scope scope = createStoreageScope(request, EnvironmentClass.valueOf(getSingleAsString(request, "scope.environmentclass", true)));
        String alias = getSingleAsString(request, "alias");
        Resource resource = new Resource(alias, resourceType, scope);
        String adGroups = getSingleAsString(request, "accessAdGroups", false);
        if (adGroups != null) {
            resource.getAccessControl().setAdGroups(adGroups);
        }

        addPropertiesToResource(request, resource);
        Resource storedResource = repo.store(resource);
        log.info("Create resource {} of type {} with scope:{} id:{}", storedResource.getAlias(), resourceType, storedResource.getScope(), storedResource.getID());
        return createResourceElement(storedResource, false);
    }

    /**
     * @param resourceId
     * @return ressursen
     * 
     * @HTTP 404 Hvis ressursen ikke finnes
     */
    @GetMapping(path = "/{resourceId}", produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResourceElement getResource(@PathVariable(name = "resourceId") Long resourceId) {
        return createResourceElement(getResourceById(resourceId), false);
    }

    /**
     * Sletter en ressurs
     * 
     * @param resourceId
     * 
     * @HTTP 404 Hvis ressursen ikke finnes
     */
    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> deleteResource(@PathVariable(name = "resourceId") Long resourceId) {
        Resource resource = getResourceById(resourceId);
        log.info("deleting resource alias:{} id:{}", resource.getAlias(), resource.getID());
        repo.delete(resource);
        return ResponseEntity.noContent().build();
    }

    /**
     * Oppdaterer en ressurs i fasit
     * 
     * 
     * @param input
     *            multipart form der hver property i ressursen har sin egen part
     * 
     * @HTTP 400 ved feil eller manglende properties
     * @HTTP 404 Hvis ressursen ikke finnes
     */
    @PostMapping(path = "/{resourceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @Transactional
    public ResourceElement updateResource(@PathVariable(name = "resourceId") Long resourceId, MultipartHttpServletRequest request) {
        Resource resource = getResourceById(resourceId);
        addPropertiesToResource(request, resource);
        log.info("updating resource alias:{} id:{}", resource.getAlias(), resource.getID());
        repo.store(resource);
        return createResourceElement(resource, false);
    }

    private Resource getResourceById(Long resourceId) {
        Resource resource;
        try {
            resource = repo.getById(Resource.class, resourceId);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource with id " + resourceId + " is not found");
        }
        return resource;
    }

    @SuppressWarnings("deprecation")
	private Resource addPropertiesToResource(MultipartHttpServletRequest request, Resource resource) {
        Scope changedScope = createStoreageScope(request, resource.getScope().getEnvClass());

        if(inputContains(request, "scope.domain")){
            resource.getScope().domain(changedScope.getDomain());
         }
        if(inputContains(request, "scope.environmentname")){
            resource.getScope().envName(changedScope.getEnvironmentName());
         }
        if(inputContains(request, "scope.application")){
            resource.getScope().application(changedScope.getApplication());
         }
        
        if(inputContains(request, "lifeCycleStatus")){
           updateStatus(resource, LifeCycleStatusDO.valueOf(getSingleAsString(request, "lifeCycleStatus")));
        }
        
        if(inputContains(request, "accessAdGroup")){
            resource.getAccessControl().setAdGroups(getSingleAsString(request, "accessAdGroup"));
         }
        
        List<PropertyField> validProperties = resource.getType().getResourcePropertyFields();
        for (PropertyField property : validProperties) {
            String propertyName = property.getName();

            switch (property.getType()) {
            case ENUM:
            case TEXT:
                if (inputContains(request, propertyName)) {
                    resource.putPropertyAndValidate(propertyName, getSingleAsString(request, propertyName));
                }
                break;
            case SECRET:
                if (inputContains(request, propertyName)) {
                    resource.putSecretAndValidate(propertyName, getSingleAsString(request, propertyName));
                }
                break;
            case FILE:
                if (inputContains(request, propertyName + ".filename")) {
                    FileEntity file = new FileEntity(getSingleAsString(request, propertyName + ".filename"), getInputStream(request, propertyName + ".file"));
                    resource.putFileAndValidate(propertyName, file);
                }
                break;

            default:
                throw new RuntimeException("Unable to handle store of resource property type " + property.getType());
            }
        }
        return resource;
    }

    private void updateStatus(Resource updateMe, LifeCycleStatusDO status) {
        if (LifeCycleStatusDO.STOPPED == status) {
            updateMe.changeStatus(LifeCycleStatus.STOPPED);
            log.info("Stopping resource {}", updateMe.getAlias());

        }
        if (LifeCycleStatusDO.STARTED == status) {
            updateMe.resetStatus();
            log.info("Starting resource {}", updateMe.getAlias());
        }
        
    }

    private boolean inputContains(MultipartHttpServletRequest request, String propertyName) {
        return request.getParameterMap().containsKey(propertyName) || request.getFileMap().containsKey(propertyName);
    }

    private ResourceElement createResourceElement(Resource resource, Boolean showUsage) {
        // MODTP-990 Need to make sure ResourceType can be mapped to a ResourceTypeDO element. If not set resourceType to null
        // This is so that we are able to return resources that are not part of the ResourceTypeDO enum such as Datapower
        ResourceTypeDO resourceType = toEnumOrNull(ResourceTypeDO.class, resource.getType().name());
        ResourceElement resourceElement = new ResourceElement(resourceType, resource.getAlias());

        URI ref = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/conf/resources/{resourceId}")
                .buildAndExpand(resource.getID())
                .toUri();
        resourceElement.setRef(ref);

        Scope resourceScope = resource.getScope();
        resourceElement.setEnvironmentClass(enumNameOrNull(resourceScope.getEnvClass()));

        if (resourceScope.getDomain() != null) {
            resourceElement.setDomain(DomainDO.valueOf(resourceScope.getDomain().name()));
        }

        Application application = resource.getScope().getApplication();
        if (application != null) {
            resourceElement.setApplication(application.getName());
        }

        resourceElement.setEnvironmentName(resourceScope.getEnvironmentName());
        resourceElement.setId(resource.getID());
        resourceElement.setDodgy(resource.isDodgy());
        resourceElement.setAccessAdGroup(resource.getAccessControl().getAdGroups());
        addPropertiesToResourceElement(resource, resourceElement);
        URI baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        addSecretsToResourceElement(resource, resourceElement, baseUrl);
        addFilesToResourceElement(resource, resourceElement, baseUrl);

        if (showUsage) {
            addUsedByApplicationInfoToElement(resource, resourceElement);
        }

        if (resource.getLifeCycleStatus() != null) {
            resourceElement.setLifeCycleStatus(toEnumOrNull(LifeCycleStatusDO.class, resource.getLifeCycleStatus().name()));
        }

        resourceElement.setRevision(getHeadRevision(resource.getID()));

        return resourceElement;
    }

    private Long getHeadRevision(Long id) {
        List<Tuple<Long, RevisionType>> history = repo.getRevisionsFor(Resource.class, id);
        return history.isEmpty() ? null : history.get(0).fst;
    }

    private void addUsedByApplicationInfoToElement(Resource resource, ResourceElement resourceElement) {

        List<ApplicationInstance> applications = applicationInstanceRepository.findApplicationInstancesUsing(resource);
        List<ApplicationInstanceDO> usedInApplications = new ArrayList<ApplicationInstanceDO>();

        for (ApplicationInstance applicationInstance : applications) {
            Environment environment = repo.getEnvironmentBy(applicationInstance.getCluster());
            ApplicationInstanceDO usedInApplication = createApplicationDO(environment, applicationInstance);
            usedInApplications.add(usedInApplication);
        }

        resourceElement.setUsedInApplication(usedInApplications);
    }

    private ApplicationInstanceDO createApplicationDO(Environment environment, ApplicationInstance instance) {
        ApplicationInstanceDO appDO = new ApplicationInstanceDO(instance.getApplication().getName(), environment.getName().toLowerCase(), ServletUriComponentsBuilder.fromCurrentContextPath());
        return appDO;
    }

    private Scope getSearchScope(String envClass, String envName, String domain, String application) {
        Scope scope = new Scope(toEnumOrNull(EnvironmentClass.class, envClass != null ? envClass.toLowerCase() : envClass));
        if (domain != null) {
            scope.domain(Domain.fromFqdn(domain));
        }
        if (envName != null) {
            scope.envName(envName.toLowerCase());
        }
        if (application != null) {
            Application app = repo.findApplicationByName(application);
            scope.application(app);
        }
        if (scope.getEnvClass() == null) {
            log.debug("envClass is not set. Trying to find class from environmentName {}", scope.getEnvironmentName());
            Environment environment = repo.findEnvironmentBy(scope.getEnvironmentName());
            if (environment != null) {
                scope.envClass(environment.getEnvClass());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to find environment with name " + envName + " You need to specify environmentName or environmentClass");
            }
        }
        return scope;
    }

    private void addFilesToResourceElement(Resource resource, ResourceElement resourceElement, URI baseUrl) {
        for (Entry<String, FileEntity> entry : resource.getFiles().entrySet()) {
            URI ref = ServletUriComponentsBuilder.fromUri(baseUrl)
                    .path(FileRestService.createPath(resource, entry.getKey()))
                    .build()
                    .toUri();
            resourceElement.addProperty(new PropertyElement(entry.getKey(), ref, Type.FILE));
        }
    }

    private void addPropertiesToResourceElement(Resource resource, ResourceElement resourceElement) {
        for (Entry<String, String> entryy : resource.getProperties().entrySet()) {
            resourceElement.addProperty(new PropertyElement(entryy.getKey(), entryy.getValue()));
        }
    }

    private void addSecretsToResourceElement(Resource resource, ResourceElement resourceElement, URI baseUrl) {
        for (Entry<String, Secret> entry : resource.getSecrets().entrySet()) {
            URI ref = ServletUriComponentsBuilder.fromUri(baseUrl)
                    .path(SecretRestService.createPath(entry.getValue()))
                    .build()
                    .toUri();
            resourceElement.addProperty(new PropertyElement(entry.getKey(), ref, Type.SECRET));
        }
    }

    private String enumNameOrNull(Enum<?> e) {
        if (e != null) {
            return e.name();
        }
        return null;
    }

    private Scope createStoreageScope(MultipartHttpServletRequest request, EnvironmentClass envclass) {
        Scope scope = new Scope(envclass);
        String domain = getSingleAsString(request, "scope.domain", false);
        if (domain != null) {
            scope.domain(Domain.fromFqdn(domain));
        }

        String envName = getSingleAsString(request, "scope.environmentname", false);
        if (envName != null && !envName.isEmpty()) {
            Environment environment = repo.findEnvironmentBy(envName.toLowerCase());
            if (environment == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Environment with name " + envName + " not found");
            }
            scope.envName(envName);
        }
        String applicationName = getSingleAsString(request, "scope.application", false);
        if (applicationName != null && !applicationName.isEmpty()) {
            Application app = repo.findApplicationByName(applicationName);
            if (app == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application with name " + applicationName + " not found");
            }
            scope.application(app);
        }

        return scope;
    }

    private InputStream getInputStream(MultipartHttpServletRequest request, String property) {
        if (request.getFileMap().containsKey(property)) {
            try {
                return request.getFile(property).getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private String getSingleAsString(MultipartHttpServletRequest request, String property) {
        return getSingleAsString(request, property, true);
    }

    private String getSingleAsString(MultipartHttpServletRequest request, String property, boolean mandatory) {
        if (!request.getParameterMap().containsKey(property)) {
            if (mandatory) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required property " + property);
            }
            return null;
        }
        return request.getParameter(property);
    }

//    private InputPart getSingle(Map<String, List<InputPart>> multipartMap, String property) {
//        return getSingle(multipartMap, property, true);
//    }

//    private InputPart getSingle(Map<String, List<InputPart>> multipartMap, String property, boolean mandatory) {
//        List<InputPart> list = multipartMap.get(property);
//        if (list == null || list.isEmpty()) {
//            if (mandatory) {
//                log.warn("Missing required property " + property);
//                throw new BadRequestException("Missing required property " + property);
//            }
//            return null;
//        }
//        if (list.size() == 1) {
//            return list.get(0);
//        } else {
//            log.warn("More than one property with name " + property + " is in request. Only one is expected");
//            throw new BadRequestException("More than one property with name " + property + " is in request. Only one is expected");
//        }
//    }

}
