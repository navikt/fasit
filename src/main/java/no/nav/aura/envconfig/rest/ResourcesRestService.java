package no.nav.aura.envconfig.rest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.ApplicationInstanceDO;
import no.nav.aura.envconfig.client.DomainDO;
import no.nav.aura.envconfig.client.LifeCycleStatusDO;
import no.nav.aura.envconfig.client.ResourceTypeDO;
import no.nav.aura.envconfig.client.rest.PropertyElement;
import no.nav.aura.envconfig.client.rest.PropertyElement.Type;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.*;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.spring.User;
import no.nav.aura.envconfig.util.SerializableFunction;
import no.nav.aura.envconfig.util.Tuple;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.RevisionType;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import static no.nav.aura.envconfig.rest.util.Converters.toEnumOrNull;

/**
 * Api for å h�ndtere ressurser i envconfig
 */
@Component
@Path("/conf/resources")
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
    @GET
    @Path("/bestmatch")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ResourceElement getBestMatchingResource(@QueryParam("envClass") String envClass, @QueryParam("envName") String envName, @QueryParam("domain") String domain,
            @QueryParam("app") String application, @QueryParam("type") ResourceTypeDO type, @QueryParam("alias") String alias, @Context UriInfo uriInfo) {

        Scope scope = getSearchScope(envClass, envName, domain, application);

        if (scope.getApplication() == null) {
            throw new BadRequestException("Bad or missing required parameter 'app'");
        }
        if (scope.getDomain() == null) {
            throw new BadRequestException("Bad or missing required parameter 'domain'");
        }

        if (scope.getEnvironmentName() == null) {
            throw new BadRequestException("Bad or missing required parameter 'envName'");
        }

        ResourceType resourceType = toEnumOrNull(ResourceType.class, enumNameOrNull(type));
        if (resourceType == null) {
            throw new BadRequestException("Bad or missing required parameter 'type'");
        }

        if (StringUtils.isEmpty(alias)) {
            throw new BadRequestException("Missing required parameter 'alias'");
        }

        ResourceElement[] resources = findResources(envClass, envName, domain, application, type, alias, true, false, uriInfo);
        if (resources.length == 0) {
            throw new NotFoundException("Found no active resources with alias " + alias + " of type " + resourceType + " in scope " + scope);
        }

        if (resources.length != 1) {
            throw new RuntimeException("Found " + resources.length + " resources with alias " + alias + " of type " + resourceType + " in scope " + scope + ". Expected only one");
        }

        return resources[0];
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

    @SuppressWarnings("serial")
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ResourceElement[] findResources(@QueryParam("envClass") String envClass, @QueryParam("envName") String envName, @QueryParam("domain") String domain,
            @QueryParam("app") String application, @QueryParam("type") ResourceTypeDO type, @QueryParam("alias") String alias, @QueryParam("bestmatch") Boolean bestmatch,
            @QueryParam("usage") @DefaultValue("false") Boolean usage, @Context final UriInfo uriInfo) {

        final Scope scope = getSearchScope(envClass, envName, domain, application);

        final Boolean showUsedInApplications = usage;

        ResourceType resourceType = toEnumOrNull(ResourceType.class, enumNameOrNull(type));
        Collection<Resource> allResourcesWithAlias = repo.findResourcesByExactAlias(scope, resourceType, alias);
        Collection<Resource> activeResources = allResourcesWithAlias;//removeStoppedResouces(allResourcesWithAlias);

        if (activeResources.isEmpty()) {
            log.debug("Found no resources with alias:{} of type:{} in {}", alias, resourceType, scope);
            return new ResourceElement[] {};
        }

        if (bestmatch != null && bestmatch) {
            activeResources = filterResultsForBestMatch(scope, activeResources);
        }

        log.info("Found {} resources with alias:{} of type:{} in {} , bestmatch: {}", activeResources.size(), alias, resourceType, scope, bestmatch);

        return FluentIterable.from(activeResources).transform(new SerializableFunction<Resource, ResourceElement>() {
            public ResourceElement process(Resource resource) {
                return createResourceElement(uriInfo, resource, showUsedInApplications);
            }
        }).toArray(ResourceElement.class);
    }

    @SuppressWarnings("serial")
    private Collection<Resource> filterResultsForBestMatch(final Scope scope, Collection<Resource> activeResources) {
        // create a multimap with all resources of a type and its name
        Multimap<String, Resource> resourcesMultiMap = ArrayListMultimap.create();
        for (Resource resource : activeResources) {
            resourcesMultiMap.put(resource.getAlias().toLowerCase() + resource.getType(), resource);
        }
        // Filter out best matching resources
        Map<String, Resource> resourcesMap = Maps.transformValues(resourcesMultiMap.asMap(), new SerializableFunction<Collection<Resource>, Resource>() {
            public Resource process(Collection<Resource> input) {
                return scope.singleBestMatch(input);
            }
        });

        return resourcesMap.values();
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
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Transactional
    public ResourceElement createResource(MultipartFormDataInput input, @Context UriInfo uriInfo) {
        Map<String, List<InputPart>> multipartMap = input.getFormDataMap();
        ResourceTypeDO resourceType = toEnumOrNull(ResourceTypeDO.class, getSingleAsString(multipartMap, "type"));
        return createResource(resourceType, input, uriInfo);
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
    @PUT
    @Path("/{type}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @Deprecated
    @Transactional
    public ResourceElement addResource(@PathParam("type") ResourceTypeDO type, MultipartFormDataInput input, @Context UriInfo uriInfo) {
        return createResource(type, input, uriInfo);
    }

    private ResourceElement createResource(ResourceTypeDO type, MultipartFormDataInput input, @Context UriInfo uriInfo) {
        ResourceType resourceType = toEnumOrNull(ResourceType.class, enumNameOrNull(type));
        Map<String, List<InputPart>> multipartMap = input.getFormDataMap();
        if (type == null) {
            throw new BadRequestException("Missing required parameter type. Legal values is: " + Arrays.asList(ResourceType.values()));
        }

        Scope scope = createStoreageScope(multipartMap, EnvironmentClass.valueOf(getSingleAsString(multipartMap, "scope.environmentclass", true)));
        String alias = getSingleAsString(multipartMap, "alias");
        Resource resource = new Resource(alias, resourceType, scope);
        String adGroups = getSingleAsString(multipartMap, "accessAdGroups", false);
        if (adGroups != null) {
            resource.getAccessControl().setAdGroups(adGroups);
        }

        addPropertiesToResource(input, resource);
        Resource storedResource = repo.store(resource);
        log.info("Create resource {} of type {} with scope:{} id:{}", storedResource.getAlias(), resourceType, storedResource.getScope(), storedResource.getID());
        return createResourceElement(uriInfo, storedResource, false);
    }

    /**
     * @param resourceId
     * @return ressursen
     * 
     * @HTTP 404 Hvis ressursen ikke finnes
     */
    @GET
    @Path("/{resourceId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ResourceElement getResource(@PathParam("resourceId") Long resourceId, @Context UriInfo uriInfo) {
        return createResourceElement(uriInfo, getResourceById(resourceId), false);
    }

    /**
     * Sletter en ressurs
     * 
     * @param resourceId
     * 
     * @HTTP 404 Hvis ressursen ikke finnes
     */
    @DELETE
    @Path("/{resourceId}")
    public void deleteResource(@PathParam("resourceId") Long resourceId) {
        Resource resource = getResourceById(resourceId);
        log.info("deleting resource alias:{} id:{}", resource.getAlias(), resource.getID());
        repo.delete(resource);
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
    @POST
    @Path("/{resourceId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Transactional
    public ResourceElement updateResource(@PathParam("resourceId") Long resourceId, MultipartFormDataInput input, @Context UriInfo uriInfo) {
        Resource resource = getResourceById(resourceId);
        addPropertiesToResource(input, resource);
        log.info("updating resource alias:{} id:{}", resource.getAlias(), resource.getID());
        repo.store(resource);
        return createResourceElement(uriInfo, resource, false);
    }

    private Resource getResourceById(Long resourceId) {
        Resource resource;
        try {
            resource = repo.getById(Resource.class, resourceId);
        } catch (NoResultException e) {
            throw new NotFoundException("Resource with id " + resourceId + " is not found");
        }
        return resource;
    }

    private Resource addPropertiesToResource(MultipartFormDataInput multipart, Resource resource) {
        Map<String, List<InputPart>> input = multipart.getFormDataMap();
        Scope changedScope = createStoreageScope(input, resource.getScope().getEnvClass());
        
        if(inputContains(input, "scope.domain")){
            resource.getScope().domain(changedScope.getDomain());
         }
        if(inputContains(input, "scope.environmentname")){
            resource.getScope().envName(changedScope.getEnvironmentName());
         }
        if(inputContains(input, "scope.application")){
            resource.getScope().application(changedScope.getApplication());
         }
        
        if(inputContains(input, "lifeCycleStatus")){
           updateStatus(resource, LifeCycleStatusDO.valueOf(getSingleAsString(input, "lifeCycleStatus")));
        }
        
        if(inputContains(input, "accessAdGroup")){
            resource.getAccessControl().setAdGroups(getSingleAsString(input, "accessAdGroup"));
         }
        
        List<PropertyField> validProperties = resource.getType().getResourcePropertyFields();
        for (PropertyField property : validProperties) {
            String propertyName = property.getName();

            switch (property.getType()) {
            case ENUM:
            case TEXT:
                if (inputContains(input, propertyName)) {
                    resource.putPropertyAndValidate(propertyName, getSingleAsString(input, propertyName));
                }
                break;
            case SECRET:
                if (inputContains(input, propertyName)) {
                    resource.putSecretAndValidate(propertyName, getSingleAsString(input, propertyName));
                }
                break;
            case FILE:
                if (inputContains(input, propertyName + ".filename")) {
                    FileEntity file = new FileEntity(getSingleAsString(input, propertyName + ".filename"), getInputStream(input, propertyName + ".file"));
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

    private boolean inputContains(Map<String, List<InputPart>> input, String propertyName) {
        return input.keySet().contains(propertyName);
    }

    private ResourceElement createResourceElement(UriInfo uriInfo, Resource resource, Boolean showUsage) {
        // MODTP-990 Need to make sure ResourceType can be mapped to a ResourceTypeDO element. If not set resourceType to null
        // This is so that we are able to return resources that are not part of the ResourceTypeDO enum such as Datapower
        ResourceTypeDO resourceType = toEnumOrNull(ResourceTypeDO.class, resource.getType().name());
        ResourceElement resourceElement = new ResourceElement(resourceType, resource.getAlias());

        resourceElement.setRef(uriInfo.getBaseUriBuilder().clone()
                .path(ResourcesRestService.class)
                .path(ResourcesRestService.class, "getResource")
                .build(resource.getID()));

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
        URI baseUrl = uriInfo.getBaseUriBuilder().build();
        addSecretsToResourceElement(resource, resourceElement, baseUrl);
        addFilesToResourceElement(resource, resourceElement, baseUrl);

        if (showUsage) {
            addUsedByApplicationInfoToElement(resource, resourceElement, uriInfo);
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

    private void addUsedByApplicationInfoToElement(Resource resource, ResourceElement resourceElement, UriInfo uriInfo) {

        List<ApplicationInstance> applications = applicationInstanceRepository.findApplicationInstancesUsing(resource);
        List<ApplicationInstanceDO> usedInApplications = new ArrayList<ApplicationInstanceDO>();

        for (ApplicationInstance applicationInstance : applications) {
            Environment environment = repo.getEnvironmentBy(applicationInstance.getCluster());
            ApplicationInstanceDO usedInApplication = createApplicationDO(environment, applicationInstance, uriInfo);
            usedInApplications.add(usedInApplication);
        }

        resourceElement.setUsedInApplication(usedInApplications);
    }

    private ApplicationInstanceDO createApplicationDO(Environment environment, ApplicationInstance instance, UriInfo uriInfo) {
        ApplicationInstanceDO appDO = new ApplicationInstanceDO(instance.getApplication().getName(), environment.getName().toLowerCase(), uriInfo.getBaseUriBuilder());
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
                throw new BadRequestException("Unable to find environment with name " + envName + " You need to specify environmentName or environmentClass");
            }
        }
        return scope;
    }

    private void addFilesToResourceElement(Resource resource, ResourceElement resourceElement, URI baseUrl) {
        for (Entry<String, FileEntity> entry : resource.getFiles().entrySet()) {
            URI ref = UriBuilder.fromUri(baseUrl).path(FileRestService.createPath(resource, entry.getKey())).build();
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
            URI ref = UriBuilder.fromUri(baseUrl).path(SecretRestService.createPath(entry.getValue())).build();
            resourceElement.addProperty(new PropertyElement(entry.getKey(), ref, Type.SECRET));
        }
    }

    private String enumNameOrNull(Enum<?> e) {
        if (e != null) {
            return e.name();
        }
        return null;
    }

    private Scope createStoreageScope(Map<String, List<InputPart>> multipartMap, EnvironmentClass envclass) {
        Scope scope = new Scope(envclass);
        String domain = getSingleAsString(multipartMap, "scope.domain", false);
        if (domain != null) {
            scope.domain(Domain.fromFqdn(domain));
        }

        String envName = getSingleAsString(multipartMap, "scope.environmentname", false);
        if (envName != null && !envName.isEmpty()) {
            Environment environment = repo.findEnvironmentBy(envName.toLowerCase());
            if (environment == null) {
                throw new BadRequestException("Environment with name " + envName + " not found");
            }
            scope.envName(envName);
        }
        String applicationName = getSingleAsString(multipartMap, "scope.application", false);
        if (applicationName != null && !applicationName.isEmpty()) {
            Application app = repo.findApplicationByName(applicationName);
            if (app == null) {
                throw new BadRequestException("Application with name " + applicationName + " not found");
            }
            scope.application(app);
        }

        return scope;
    }

    private InputStream getInputStream(Map<String, List<InputPart>> multipartMap, String property) {
        InputPart input = getSingle(multipartMap, property);
        if (input != null) {
            try {
                return input.getBody(InputStream.class, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private String getSingleAsString(Map<String, List<InputPart>> multipartMap, String property) {
        return getSingleAsString(multipartMap, property, true);
    }

    private String getSingleAsString(Map<String, List<InputPart>> multipartMap, String property, boolean mandatory) {
        InputPart input = getSingle(multipartMap, property, mandatory);
        if (input != null) {
            try {
                return input.getBodyAsString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private InputPart getSingle(Map<String, List<InputPart>> multipartMap, String property) {
        return getSingle(multipartMap, property, true);
    }

    private InputPart getSingle(Map<String, List<InputPart>> multipartMap, String property, boolean mandatory) {
        List<InputPart> list = multipartMap.get(property);
        if (list == null || list.isEmpty()) {
            if (mandatory) {
                log.warn("Missing required property " + property);
                throw new BadRequestException("Missing required property " + property);
            }
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        } else {
            log.warn("More than one property with name " + property + " is in request. Only one is expected");
            throw new BadRequestException("More than one property with name " + property + " is in request. Only one is expected");
        }
    }

}
