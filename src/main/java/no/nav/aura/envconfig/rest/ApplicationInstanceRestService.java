package no.nav.aura.envconfig.rest;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import no.nav.aura.appconfig.Application;
import no.nav.aura.appconfig.Selftest;
import no.nav.aura.appconfig.exposed.*;
import no.nav.aura.appconfig.resource.EnvironmentDependentResource;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.*;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.util.LoadBalancerHostnameBuilder;
import no.nav.aura.envconfig.util.SerializableFunction;
import no.nav.aura.envconfig.util.Tuple;
import no.nav.aura.integration.VeraRestClient;
import no.nav.aura.sensu.SensuClient;
import org.hibernate.envers.RevisionType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Maps.filterKeys;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.aura.envconfig.util.IpAddressResolver.resolveIpFrom;

/**
 * API for applikasjonsinstances
 */
@Path("/conf/environments/{environmentName}/applications")
@Component
public class ApplicationInstanceRestService {
    @Inject
    private FasitRepository repo;

    @Inject
    private SensuClient sensuClient;

    @Inject
    VeraRestClient vera;

    @Context
    private UriInfo uriInfo;

    private static final Logger log = LoggerFactory.getLogger(ApplicationInstanceRestService.class);

    private final ImmutableMap<Class<? extends ExposedService>, ResourceType> exposedServiceToResourceMapping = ImmutableMap.of(
            ExposedSoap.class, ResourceType.WebserviceEndpoint,
            ExposedEjb.class, ResourceType.EJB,
            ExposedRest.class, ResourceType.RestService,
            ExposedUrl.class, ResourceType.BaseUrl);

    protected ApplicationInstanceRestService() {
        // For cglib and @transactional
    }

    public ApplicationInstanceRestService(FasitRepository repo, SensuClient sensuClient, VeraRestClient vera) {
        this.repo = repo;
        this.sensuClient = sensuClient;
        this.vera = vera;
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public List<ApplicationInstanceDO> getApplicationinstances(@PathParam("environmentName") final String envName) {
        List<ApplicationInstanceDO> instances = new ArrayList<>();
        Environment environment = findEnvironment(envName);

        for (ApplicationInstance applicationInstance : environment.getApplicationInstances()) {
            long startCreateDo = System.currentTimeMillis();
            ApplicationInstanceDO applicationInstanceDO = createApplicationDO(environment, applicationInstance);
            instances.add(applicationInstanceDO);
            long stopCreateDo = System.currentTimeMillis();
            long createDoTime = stopCreateDo - startCreateDo;
        }

        return instances;

    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Path("/{applicationName}")
    public ApplicationInstanceDO getApplicationinstance(@PathParam("environmentName") final String envName, @PathParam("applicationName") final String appName) {
        Environment environment = findEnvironment(envName);
        ApplicationInstance applicationInstance = findApplicationInstance(appName, environment);
        ApplicationInstanceDO applicationInstanceDO = createApplicationDO(environment, applicationInstance);
        return applicationInstanceDO;
    }

    private ApplicationInstanceDO createApplicationDO(Environment environment, ApplicationInstance instance) {
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

    /**
     * Finner cluster av noder for en gitt applikasjon i ett gitt miljø
     * 
     * @param envName
     *            navnet på miljøet
     * @param appName
     *            navnet på applikasjonen
     * @return nodene
     * 
     * @HTTP 404 Hvis miljøet ikke finnes
     * @HTTP 404 Hvis applikasjonen ikke har noen definerte clustert i gitt miljø
     * @deprecated 21.3 2014 Bruk getApplicationInstance i stedet
     */
    @GET
    @Path("/{applicationName}/clusters")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Deprecated
    public ClusterDO[] getClustersForApplication(@PathParam("environmentName") String envName, @PathParam("applicationName") String appName) {
        Environment environment = findEnvironment(envName);

        ApplicationInstance instance = findApplicationInstance(appName, environment);
        Cluster cluster = instance.getCluster();
        ClusterDO clusterDO = createClusterDO(uriInfo, environment, cluster);
        return new ClusterDO[] { clusterDO };
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
        c.setApplications(transform(cluster.getApplications()));
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

    private Collection<String> transform(Collection<no.nav.aura.envconfig.model.application.Application> applications) {
        Collection<String> apps = Collections2.transform(applications, new Function<no.nav.aura.envconfig.model.application.Application, String>() {

            @Override
            public String apply(no.nav.aura.envconfig.model.application.Application input) {
                return input.getName();
            }
        });
        return apps;
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Path("{applicationName}/appconfig")
    public String getAppConfig(@PathParam("environmentName") final String envName, @PathParam("applicationName") final String appName) {
        Environment environment = findEnvironment(envName);
        ApplicationInstance applicationInstance = findApplicationInstance(appName, environment);
        return applicationInstance.getAppconfigXml();
    }

    @Deprecated
    @PUT
    @Path("/{applicationName}")
    @Consumes(MediaType.APPLICATION_XML)
    public void registerDeployedApplication(@PathParam("environmentName") final String envName, @PathParam("applicationName") final String appName, final DeployedApplicationDO container) {
        if (!appName.equals(container.getAppconfig().getName())) {
            throw new IllegalArgumentException("Path application name " + appName + " and application config name " + container.getAppconfig().getName() + " does not match");
        }

        sensuClient.sendEvent("fasit.deprecatedAppInstanceService", Collections.emptyMap(), ImmutableMap.of("appName", appName, "envName", envName));

        Environment environment = findEnvironment(envName);
        ApplicationInstance appInstance = findApplicationInstance(appName, environment);
        Cluster cluster = appInstance.getCluster();
        Application application = container.getAppconfig();

        if (cluster.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Unable to register application " + appName + " with no nodes in environment " + envName);
        }

        if (environment.getEnvClass() == EnvironmentClass.u && !cluster.getNodes().isEmpty()) {
            cluster.setLoadBalancerUrl(UriBuilder.fromPath("").host(cluster.getNodes().iterator().next().getHostname()).scheme("https").port(cluster.getHttpsPortFromPlatformType()).build().toString());
        } else if (loadBalancerInfoIsDefinedInAppConfig(application) && environmentIsLoadBalanced(environment, appInstance)) {
            cluster.setLoadBalancerUrl(format("https://%s", LoadBalancerHostnameBuilder.create(appInstance.getDomain(), environment.getName())));
        }

        registerUsedResources(container.getUsedResources(), container.getAppconfig().getResources(EnvironmentDependentResource.class), appInstance, environment);

        deleteRemovedExposedResources(application.getExposedServices(), appInstance);

        registerExposedServices(application, environment, appInstance, cluster);

        appInstance.setVersion(container.getVersion());
        String selftestPage = getSelftest(application);

        if (selftestPage != null) {
            appInstance.setSelftestPagePath(selftestPage.trim());
        }

        appInstance.setAppconfigXml(application.asXml());
        appInstance.setDeployDate(DateTime.now());

        ApplicationInstance savedAppInstance = repo.store(appInstance);
        sensuClient.sendEvent("fasit.deployments", ImmutableMap.of("deployedApplication", appName, "targetEnvironment", envName, "targetEnvironmentClass", System.getProperty("environment.class", "u")), ImmutableMap.of("version", appInstance.getVersion()));
        vera.notifyVeraOfDeployment(savedAppInstance, environment);
    }

    private String getSelftest(Application application) {
        Selftest selftest = application.getSelftest();
        if (selftest != null) {
            if (selftest.getHumanReadablePath() != null) {
                return selftest.getHumanReadablePath();
            }

            if(selftest.getPath() != null) {
                return selftest.getPath();
            }
        }
        return null;
    }

    private boolean environmentIsLoadBalanced(Environment environment, ApplicationInstance appInstance) {
        Scope scope = new Scope(environment).domain(appInstance.getCluster().getDomain()).application(appInstance.getApplication());
        return !repo.findResourcesByExactAlias(scope, ResourceType.LoadBalancer, "bigip").isEmpty();
    }

    private boolean loadBalancerInfoIsDefinedInAppConfig(Application appconfig) {
        return appconfig != null && appconfig.getLoadBalancer() != null && appconfig.getLoadBalancer().getContextRoots() != null;
    }

    @DELETE
    @Produces({ MediaType.APPLICATION_XML })
    @Path("/{applicationName}")
    @Transactional
    public void undeployApplication(@PathParam("environmentName") final String envName, @PathParam("applicationName") final String appName) {
        Environment environment = findEnvironment(envName);
        ApplicationInstance instance = findApplicationInstance(appName, environment);
        Set<ResourceReference> resourceReferences = instance.getResourceReferences();
        resourceReferences.clear();

        // Delting all exposed services
        deleteRemovedExposedResources(new HashSet<ExposedService>(), instance);

        instance.setVersion(null);
        repo.store(instance);
    }

    /**
     * Registerer en deployet applikasjon
     * 
     * @param envName
     *            milj�et
     * @param appName
     *            applikasjonen
     * @param version
     *            versjon av applikasjonen
     * 
     * @deprecated 18.3 2014
     */
    @POST
    @Path("/{app}/versions/{version}")
    @Consumes(MediaType.APPLICATION_XML)
    @Transactional
    @Deprecated
    public void registerApplicationInstance(@PathParam("environmentName") final String envName, @PathParam("app") final String appName, @PathParam("version") final String version,
            final DeployedApplicationDO container) {
        container.setVersion(version);
        registerDeployedApplication(envName, appName, container);
    }

    /**
     * Verifiserer at en applikasjon er deploybar fra fasit sin side
     * 
     * @param envName
     *            milj�et
     * @param appName
     *            applikasjonen
     * @param newApplication
     *            xmlversjon av appconfig for applikasjonen eller null om applikasjonen blir avinstallert.
     * 
     */
    @PUT
    @Path("/{applicationName}/verify")
    @Consumes(MediaType.APPLICATION_XML)
    @Transactional
    public void verifyApplicationInstance(@PathParam("environmentName") final String envName, @PathParam("applicationName") final String appName, Application newApplication) {
        Environment environment = findEnvironment(envName);
        ApplicationInstance appInstance = findApplicationInstance(appName, environment);
        if (newApplication == null) {
            throw new BadRequestException("No application content provided in request");
        }
        checkIfExposedServiceExistsForOtherApplications(environment, appInstance, newApplication);
    }

    private void checkIfExposedServiceExistsForOtherApplications(Environment environment, ApplicationInstance instance, Application newApplication) {
        Collection<ExposedService> exposedServices = newApplication.getExposedServices();
        for (ExposedService exposedService : exposedServices) {
            Domain currentDomain = instance.getCluster().getDomain();
            Scope resourceSearchScope = new Scope(environment).application(instance.getApplication()).domain(currentDomain);
            List<Resource> existingResources = repo.findResourcesByExactAlias(resourceSearchScope, findResourceTypeForExposedService(exposedService), exposedService.getName());
            Domain expectedDomain = calculateDomain(exposedService, instance);
            for (Resource resource : existingResources) {
                if (sameScope(environment, expectedDomain, resource)) {
                    ApplicationInstance applicationInstanceForExposedService = repo.findApplicationInstanceByExposedResourceId(resource.getID());
                    if (notSameApplication(newApplication, applicationInstanceForExposedService) && !(ResourceTypeDO.Queue.name().equals(resource.getType().name()))) {
                        throw new BadRequestException(format(
                                "The resource %s of type %s already exists in Fasit and is not exposed by application %s. A resource can not be registrered more than once in the same scope %s ",
                                resource.getName(), resource.getType(), newApplication.getName(), resource.getScope()));
                    }
                }
            }
        }
    }

    private boolean notSameApplication(Application newApplication, ApplicationInstance applicationInstanceForExposedService) {
        return applicationInstanceForExposedService == null || !newApplication.getName().equals(applicationInstanceForExposedService.getApplication().getName());
    }

    private boolean sameScope(Environment expectedEnvironment, Domain expectedDomain, Resource resource) {
        boolean domainMatches = Objects.equals(expectedDomain, resource.getScope().getDomain());
        boolean envionmentMatches = expectedEnvironment.getName().equalsIgnoreCase(resource.getScope().getEnvironmentName());
        return envionmentMatches && domainMatches;
    }

    private ResourceType findResourceTypeForExposedService(ExposedService exposedService) {
        return exposedServiceToResourceMapping.get(exposedService.getClass());
    }

    private void registerUsedResources(Set<ResourceElement> usedResources, Collection<EnvironmentDependentResource> declaredResourcesInAppconfig, ApplicationInstance instance, Environment environment) {
        ImmutableSet<ResourceReference> existingResourceReferences = ImmutableSet.copyOf(instance.getResourceReferences());
        Set<ResourceReference> newResourceReferences = new HashSet<>();

        // Update or modify resources
        for (ResourceElement resourceElement : usedResources) {
            Resource resource = repo.getById(Resource.class, resourceElement.getId());
            newResourceReferences.add(findReferenceOrCreateNew(existingResourceReferences, resource));
        }
        // Create future resources for non-existing resource defined in appconfig
        for (EnvironmentDependentResource declaredResource : declaredResourcesInAppconfig) {
            if (ResourceTypeDO.isDefinedFor(declaredResource)) {
                ResourceType resourceType = ResourceType.valueOf(ResourceTypeDO.findTypeFor(declaredResource).name());
                if (!containsReferenceWithNameAndType(newResourceReferences, declaredResource.getAlias(), resourceType)) {
                    log.info("Creating future resourcereference {} for application {} in environment {}", declaredResource.getAlias(), instance.getName(), environment.getName());
                    newResourceReferences.add(ResourceReference.future(declaredResource.getAlias(), resourceType));
                }
            }
        }

        Set<ResourceReference> resourceReferences = instance.getResourceReferences();
        resourceReferences.clear();
        log.info("Updated {} resourcereferences for application {} in environment {}", newResourceReferences.size(), instance.getName(), environment.getName());
        resourceReferences.addAll(newResourceReferences);
    }

    private boolean containsReferenceWithNameAndType(Set<ResourceReference> resourceReferences, String alias, ResourceType resourceType) {
        for (ResourceReference resourceReference : resourceReferences) {
            if (resourceReference.getResourceType().equals(resourceType) && resourceReference.getAlias().equalsIgnoreCase(alias)) {
                return true;
            }
        }
        return false;
    }

    private ResourceReference findReferenceOrCreateNew(Set<ResourceReference> existingResourceReferences, Resource resource) {
        for (ResourceReference resourceReference : existingResourceReferences) {
            if (resource.equals(resourceReference.getResource())) {
                resourceReference.setAlias(resource.getAlias());
                resourceReference.setRevision(getHeadRevisionOrNull(resource));
                log.debug("Updating existing resource reference for {} ", resource.getAlias());
                return resourceReference;
            }
        }
        log.debug("Creating new resource reference for {} ", resource.getAlias());
        return new ResourceReference(resource, getHeadRevisionOrNull(resource));
    }

    private Long getHeadRevisionOrNull(Resource resource) {
        List<Tuple<Long, RevisionType>> history = repo.getRevisionsFor(Resource.class, resource.getID());
        return history.isEmpty() ? null : history.get(0).fst;
    }

    private void deleteRemovedExposedResources(Collection<ExposedService> exposedServices, ApplicationInstance instance) {
        Collection<ExposedServiceReference> candidatesForDeletion = findCandidatesForDeletion(instance.getExposedServices(), exposedServices);
        for (ExposedServiceReference exposedServiceReference : candidatesForDeletion) {
            Resource resource = exposedServiceReference.getResource();
            log.info("Deleting resource of type {} with alias {} because it is removed from application {}", resource.getType(), resource.getAlias(), instance.getName());
            instance.getExposedServices().remove(exposedServiceReference);
            repo.delete(exposedServiceReference);
        }
    }

    private void registerExposedServices(Application application, Environment environment, ApplicationInstance instance, Cluster cluster) {
        Map<String, ExposedServiceReference> existingResources = new HashMap<>();
        for (ExposedServiceReference exposedServiceReference : instance.getExposedServices()) {
            existingResources.put(exposedServiceReference.getResourceAlias(), exposedServiceReference);
        }
        registerExposedSoap(application, environment, instance, existingResources);
        registerExposedEjbs(application, environment, instance, existingResources, cluster);
        registerExposedRest(application, environment, instance, existingResources, cluster);
        registerExposedUrl(application, environment, instance, existingResources, cluster);
    }

    private void registerExposedEjbs(Application application, Environment environment, ApplicationInstance instance, Map<String, ExposedServiceReference> existingResources, Cluster cluster) {

        no.nav.aura.envconfig.model.application.Application appInstance = repo.findApplicationByName(application.getName());

        int baseBootstrapPort = cluster.getBaseBootstrapPortFromPlatformType() + appInstance.getPortOffset();
        String providerUrls = UriBuilder.fromPath("").host(cluster.getNodes().iterator().next().getHostname()).scheme("iiop").port(baseBootstrapPort).build().toString();

        for (ExposedEjb ejb : application.getExposedServices(ExposedEjb.class)) {
            ExposedServiceReference ejbEndpoint;
            if (existingResources.containsKey(ejb.getName())) {
                ejbEndpoint = existingResources.get(ejb.getName());
            } else {
                ejbEndpoint = new ExposedServiceReference(new Resource(ejb.getName(), ResourceType.EJB, environment.getScope()), null);
            }

            Resource ejbResource = ejbEndpoint.getResource();

            ejbResource.putPropertyAndValidate("providerUrl", providerUrls);
            ejbResource.putPropertyAndValidate("jndi", ejb.getJndi());
            ejbResource.putPropertyAndValidate("beanHomeInterface", ejb.getBeanHomeInterface());
            ejbResource.putPropertyAndValidate("beanComponentInterface", ejb.getBeanComponentInterface());
            ejbResource.putPropertyAndValidate("description", ejb.getDescription());
            ejbResource.getScope().domain(instance.getCluster().getDomain());

            checkResolvedFutureResourceReferences(ejbResource, environment);

            if (!existingResources.containsKey(ejb.getName())) {
                instance.getExposedServices().add(ejbEndpoint);
            }

            log.info("Ejb {} is registered by application {}", ejb.getName(), application.getName());
        }
    }

    private void registerExposedRest(Application application, Environment environment, ApplicationInstance instance, Map<String, ExposedServiceReference> existingResources, Cluster cluster) {
        String loadBalancerUrl = getLoadBalancerUrl(instance, environment);

        for (ExposedRest exposedRest : application.getExposedServices(ExposedRest.class)) {
            ExposedServiceReference restService;
            if (existingResources.containsKey(exposedRest.getName())) {
                restService = existingResources.get(exposedRest.getName());
            } else {
                restService = new ExposedServiceReference(new Resource(exposedRest.getName(), ResourceType.RestService, environment.getScope()), null);
            }

            Resource restServiceResource = restService.getResource();
            restServiceResource.putPropertyAndValidate("url", UriBuilder.fromUri(loadBalancerUrl).path(exposedRest.getPath()).build().toString());
            restServiceResource.putPropertyAndValidate("description", exposedRest.getDescription());
            restServiceResource.getScope().domain(instance.getCluster().getDomain());

            checkResolvedFutureResourceReferences(restServiceResource, environment);

            if (!existingResources.containsKey(exposedRest.getName())) {
                instance.getExposedServices().add(restService);
            }

            log.info("Exposed url {} is registered by application {}", restService.getName(), application.getName());
        }
    }

    public String getLoadBalancerUrl(ApplicationInstance instance, Environment environment) {
        String loadBalancerUrl = instance.getCluster().getLoadBalancerUrl();
        if (isNull(loadBalancerUrl)) {
            return null;
        }
        return EnvironmentClass.u.equals(environment.getEnvClass()) && ! loadBalancerUrl.matches(".*:\\d{4}.*") ?
                loadBalancerUrl + ":" + instance.getHttpsPort() : loadBalancerUrl;
    }

    private void registerExposedUrl(Application application, Environment environment, ApplicationInstance instance, Map<String, ExposedServiceReference> existingResources, Cluster cluster) {
        String loadBalancerUrl = getLoadBalancerUrl(instance, environment);

        for (ExposedUrl exposedUrl : application.getExposedServices(ExposedUrl.class)) {
            ExposedServiceReference urlService;
            if (existingResources.containsKey(exposedUrl.getName())) {
                urlService = existingResources.get(exposedUrl.getName());
            } else {
                urlService = new ExposedServiceReference(new Resource(exposedUrl.getName(), ResourceType.BaseUrl, environment.getScope()), null);
            }
            Resource urlResource = urlService.getResource();
            urlResource.putPropertyAndValidate("url", UriBuilder.fromUri(loadBalancerUrl).path(exposedUrl.getPath()).build().toString());
            urlResource.getScope().domain(calculateDomain(exposedUrl, instance));

            checkResolvedFutureResourceReferences(urlResource, environment);

            if (!existingResources.containsKey(exposedUrl.getName())) {
                instance.getExposedServices().add(urlService);
            }

            log.info("Exposed url {} is registered by application {}", exposedUrl.getName(), application.getName());
        }
    }

    private void registerExposedSoap(Application application, Environment environment, ApplicationInstance instance, Map<String, ExposedServiceReference> existingResources) {
        String loadBalancerUrl = getLoadBalancerUrl(instance, environment);

        for (ExposedSoap webService : application.getExposedServices(ExposedSoap.class)) {
            ExposedServiceReference webserviceEndpoint;
            if (existingResources.containsKey(webService.getName())) {
                webserviceEndpoint = existingResources.get(webService.getName());
            } else {
                webserviceEndpoint = new ExposedServiceReference(new Resource(webService.getName(), ResourceType.WebserviceEndpoint, environment.getScope()), null);
            }

            Resource webserviceResource = webserviceEndpoint.getResource();
            if (webService instanceof ExposedSoap) {
                webserviceResource.putPropertyAndValidate("securityToken", fromNullable(webService.getSecurityToken()).or(SecurityToken.SAML).name());
            } else {
                webserviceResource.putPropertyAndValidate("securityToken", fromNullable(webService.getSecurityToken()).or(SecurityToken.NONE).name());
            }

            webserviceResource.putPropertyAndValidate("endpointUrl", UriBuilder.fromUri(loadBalancerUrl).path(webService.getPath()).build().toString());
            if (!(webService.getWsdlArtifactId() == null)) {
                webserviceResource.putPropertyAndValidate("wsdlUrl",
                        UriBuilder.fromUri("http://maven.adeo.no/nexus/content/groups/public").path(webService.getWsdlGroupId().replaceAll("\\.", "\\/")).path("{artifact}")
                                .path("{version}").path("{artifact}-{version}.zip").build(webService.getWsdlArtifactId(), webService.getWsdlVersion()).toString());
            }
            webserviceResource.putPropertyAndValidate("description", webService.getDescription());
            webserviceResource.getScope().domain(calculateDomain(webService, instance));

            checkResolvedFutureResourceReferences(webserviceResource, environment);

            if (!existingResources.containsKey(webService.getName())) {
                instance.getExposedServices().add(webserviceEndpoint);
            }

            log.info("Webservice {} is registered by application {}", webService.getName(), application.getName());
        }
    }

    private void checkResolvedFutureResourceReferences(Resource resource, Environment deployedToEnv) {
        Collection<ResourceReference> futureReferences = repo.findFutureResourceReferencesBy(resource.getAlias(), resource.getType());
        for (ResourceReference future : futureReferences) {
            ApplicationInstance futureApplication = repo.getApplicationInstanceBy(future);
            Environment futureEnvironment = repo.getEnvironmentBy(futureApplication);
            Scope futureScope = new Scope(futureEnvironment).domain(futureApplication.getCluster().getDomain());

            if (futureScope.isSubsetOf(resource.getScope())) {
                future.setResource(resource);
                log.info("Updating future reference {} for {} in {} ", resource.getName(), futureApplication.getName(), futureEnvironment.getName());
                repo.store(future);
            }
        }
    }

    private Domain calculateDomain(ExposedService exposedService, ApplicationInstance instanse) {
        Domain currentDomain = instanse.getCluster().getDomain();
        if (exposedService.exportTo(NetworkZone.ALL)) {
            return null;
        }
        if (!exposedService.getExportToZones().isEmpty() && currentDomain.isInZone(Zone.FSS)) {
            if (exposedService.exportTo(NetworkZone.SBS)) {
                log.info("Webservice {} is exposed to SBS, setting domain scope to all domains", exposedService.getName());
                return null;
            }
        }
        return currentDomain;
    }

    private Collection<ExposedServiceReference> findCandidatesForDeletion(Collection<ExposedServiceReference> exposedServices, Collection<ExposedService> newServices) {
        @SuppressWarnings("serial")
        ImmutableMap<String, ExposedServiceReference> exposedServiceReferences = uniqueIndex(exposedServices, new SerializableFunction<ExposedServiceReference, String>() {
            public String process(ExposedServiceReference input) {
                return input.getResourceAlias();
            }
        });

        Set<String> newServiceNames = Sets.newHashSet();
        for (ExposedService service : newServices) {
            newServiceNames.add(service.getName());
        }

        return filterKeys(exposedServiceReferences, not(in(newServiceNames))).values();
    }

    protected ApplicationInstance findApplicationInstance(final String appName, Environment environment) {
        ApplicationInstance appInstance = environment.findApplicationByName(appName);
        if (appInstance == null) {
            throw new NotFoundException("Application " + appName + " is not defined in environment " + environment.getName());
        }
        return appInstance;
    }

    protected Environment findEnvironment(String envName) {
        Environment environment = repo.findEnvironmentBy(envName.toLowerCase());
        if (environment == null) {
            throw new NotFoundException("Environment " + envName + " not found");
        }
        return environment;
    }
}
