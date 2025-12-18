package no.nav.aura.envconfig.rest;

import com.google.common.base.Predicate;
import no.nav.aura.appconfig.exposed.ExposedSoap;
import no.nav.aura.appconfig.exposed.NetworkZone;
import no.nav.aura.appconfig.resource.Webservice;
import no.nav.aura.envconfig.client.DeployedApplicationDO;
import no.nav.aura.envconfig.client.ResourceTypeDO;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringTest;
import no.nav.aura.integration.VeraRestClient;
import no.nav.aura.sensu.SensuClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.collect.Iterables.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ApplicationInstanceRestServiceUsedResourcesTest extends SpringTest {

    private final String loadBalancer = "https://myloadbalancer.adeo.no";
    private ApplicationInstanceRestService service;
    private Environment env;
    private ApplicationInstance applicationInstance;

    @BeforeEach
    public void setup() throws Exception {
        service = new ApplicationInstanceRestService(repository, mock(SensuClient.class), mock(VeraRestClient.class));
        env = new Environment("test", EnvironmentClass.t);
        Cluster cluster = new Cluster("myCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl(loadBalancer);
        env.addCluster(cluster);
        Application app = new Application("app", "app", "no.nav.app");
        cluster.addApplication(app);
        cluster.addNode(new Node("hostname.test.local", "username", "password"));
        env.addCluster(cluster);
        env = repository.store(env);
        repository.store(new Resource("appUser", ResourceType.Credential, new Scope(EnvironmentClass.t)));

        repository.store(new Resource("appDb2", ResourceType.DataSource, new Scope(EnvironmentClass.t)));

        Set<ApplicationInstance> applicationInstances = repository.findEnvironmentBy("test").getApplicationInstances();
        assertEquals(1, applicationInstances.size());
        applicationInstance = applicationInstances.iterator().next();
        assertNull(applicationInstance.getVersion());
    }

    @Test
    public void verifyingApplicationNoError() {

        Resource dependentWebservice = new Resource("otherws", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t));
        applicationInstance.getExposedServices().add(new ExposedServiceReference(dependentWebservice, null));
        repository.store(applicationInstance);

        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        service.verifyApplicationInstance("test", "app", newApplication);
    }

    @Test
    public void registerApplicationWithResources() {

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        application.getResources().add(createWebservice("futureWS"));

        DeployedApplicationDO container = new DeployedApplicationDO(application, "1.0");
        Resource datasource1 = repository.store(new Resource("appDb1", ResourceType.DataSource, new Scope(EnvironmentClass.t)));
        container.addUsedResources(resourceElementFrom(datasource1));
        Resource baseUrl = repository.store(new Resource("appBaseUrl", ResourceType.BaseUrl, new Scope(EnvironmentClass.t)));
        container.addUsedResources(resourceElementFrom(baseUrl));
        service.registerDeployedApplication("test", "app", container);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(4, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("appBaseUrl")));
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("appDb1")));
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsFutureResource("futureWS")), "futureWS not present");
        assertNotNull(newApplicationInstance.getAppconfigXml(), "appconfig xml");
        assertThat(newApplicationInstance.getAppconfigXml(), Matchers.containsString("name>app<"));
    }

    @Test
    public void registerApplicationWithWebserviceAndServiceGateway() {

        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));
        Resource serviceGateway = repository.store(new Resource("serviceGateway", ResourceType.WebserviceGateway, new Scope(EnvironmentClass.u)));

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        application.getResources().add(createWebservice("myWS"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources(resourceElementFrom(myWS), resourceElementFrom(serviceGateway));
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(5, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("myWS")));
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("serviceGateway")));
    }

    @Test
    public void registerApplicationWithWebservice() {

        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        application.getResources().add(createWebservice("myWS"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources(resourceElementFrom(myWS));
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(4, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("myWS")));
    }

    @Test
    public void registerApplicationWithUsedResource_checkCaseIncencitivity() {

        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        application.getResources().add(createWebservice("myws"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources(resourceElementFrom(myWS));
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("myWS")));
    }

    @Test
    public void registerAppWithDuplicatesShouldNotContainDuplicates() {
        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));

        no.nav.aura.appconfig.Application application = new no.nav.aura.appconfig.Application();
        application.setName("app");
        application.getResources().add(createWebservice("myWs"));
        application.getResources().add(createWebservice("MYWS"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources(resourceElementFrom(myWS), resourceElementFrom(myWS));
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(1, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("myWS")));
    }

    @Test
    public void registerAppWithDuplicatesShouldNotContainDuplicateFutures() {
        no.nav.aura.appconfig.Application application = new no.nav.aura.appconfig.Application();
        application.setName("app");
        application.getResources().add(createWebservice("myWs"));
        application.getResources().add(createWebservice("myWs"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(1, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsFutureResource("myWS")));
    }

    @Test
    public void registerApplicationAndUpdateFuturesInsameEnvironment() {
        // Deploy application with future reference
        Cluster consumerCluster = new Cluster("consumerCluster", Domain.TestLocal);
        consumerCluster.setLoadBalancerUrl("https://lb.test.local");
        consumerCluster.addNode(new Node("a.test.local", "user", "password"));
        Application consumerApp = new Application("consumer", "app", "no.nav.app");
        ApplicationInstance consumerApplicationInstance = consumerCluster.addApplication(consumerApp);
        env.addCluster(consumerCluster);
        repository.store(env);

        no.nav.aura.appconfig.Application consumerAppConfig = new no.nav.aura.appconfig.Application();
        consumerAppConfig.setName("consumer");
        consumerAppConfig.getResources().add(createWebservice("myWS"));
        service.registerDeployedApplication("test", "consumer", new DeployedApplicationDO(consumerAppConfig, "1.0"));
        ApplicationInstance storedAppinstance = repository.getById(ApplicationInstance.class, consumerApplicationInstance.getID());
        assertTrue(any(storedAppinstance.getResourceReferences(), containsFutureResource("myWS")));

        // Deploy application with service and change futures to implementation
        no.nav.aura.appconfig.Application producerAppConfig = new no.nav.aura.appconfig.Application();
        producerAppConfig.setName("app");
        ExposedSoap exposedWS = new ExposedSoap();
        exposedWS.setName("myWS");
        exposedWS.setPath("from/here/to/there");
        exposedWS.setWsdlArtifactId("id");
        exposedWS.setWsdlGroupId("group");
        exposedWS.setWsdlVersion("version");
        exposedWS.exportTo(NetworkZone.SBS);
        producerAppConfig.getExposedServices().add(exposedWS);
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(producerAppConfig, "1.0"));

        storedAppinstance = repository.getById(ApplicationInstance.class, consumerApplicationInstance.getID());
        assertTrue(any(storedAppinstance.getResourceReferences(), containsResource("myWS")));
    }

    @Test
    public void registerApplicationAndDoNotUpdateFuturesInDifferentEnvironment() {
        // Deploy application with future reference
        Cluster consumerCluster = new Cluster("consumerCluster", Domain.Devillo);
        consumerCluster.setLoadBalancerUrl("https://lb.test.local");
        consumerCluster.addNode(new Node("test.devillo.no", "user", "password"));
        Application consumerApp = repository.store(new Application("consumer", "app", "no.nav.app"));
        consumerCluster.addApplication(consumerApp);
        Environment env2 = new Environment("test2", EnvironmentClass.t);
        env2.addCluster(consumerCluster);
        repository.store(env2);
        repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t)));

        no.nav.aura.appconfig.Application consumerAppConfig = new no.nav.aura.appconfig.Application();
        consumerAppConfig.setName("consumer");
        consumerAppConfig.getResources().add(createWebservice("myWS"));
        service.registerDeployedApplication("test2", "consumer", new DeployedApplicationDO(consumerAppConfig, "1.0"));

        ApplicationInstance storedAppinstance = repository.findApplicationInstancesBy(consumerApp).get(0);
        assertTrue(any(storedAppinstance.getResourceReferences(), containsFutureResource("myWS")));

        // Deploy application with service and change futures to implementation
        no.nav.aura.appconfig.Application producerAppConfig = new no.nav.aura.appconfig.Application();
        producerAppConfig.setName("app");
        ExposedSoap exposedWS = new ExposedSoap();
        exposedWS.setName("myWS");
        exposedWS.setPath("from/here/to/there");
        exposedWS.setWsdlArtifactId("id");
        exposedWS.setWsdlGroupId("group");
        exposedWS.setWsdlVersion("version");
        producerAppConfig.getExposedServices().add(exposedWS);
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(producerAppConfig, "1.0"));

        storedAppinstance = repository.findApplicationInstancesBy(consumerApp).get(0);
        assertTrue(any(storedAppinstance.getResourceReferences(), containsFutureResource("myWS")));
    }

    @Test
    public void registerAppWithNotExistingResourceShouldCreateFutureResourceReference() {
        assertEquals(0, applicationInstance.getResourceReferences().size(), "Has no used resources");

        no.nav.aura.appconfig.Application application = new no.nav.aura.appconfig.Application();
        application.setName("app");
        application.getResources().add(createWebservice("myWs"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources();
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(1, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsFutureResource("myWs")));
    }

    @Test
    public void registerAppWithNewUsedResourceShouldCreateNewResourceReference() {
        assertEquals(0, applicationInstance.getResourceReferences().size(), "Has no used resources");

        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));
        no.nav.aura.appconfig.Application application = new no.nav.aura.appconfig.Application();
        application.setName("app");
        application.getResources().add(createWebservice("myWs"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources(resourceElementFrom(myWS));
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(1, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("myWS")));
    }

    @Test
    public void registerAppWithSameUsedResourceShouldNotCreateNewResourceReference() {
        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));
        applicationInstance.getResourceReferences().add(new ResourceReference(myWS, 1L));
        applicationInstance = repository.store(applicationInstance);
        assertEquals(1, applicationInstance.getResourceReferences().size(), "Has 1 used resources");
        ResourceReference resReference = applicationInstance.getResourceReferences().iterator().next();

        no.nav.aura.appconfig.Application application = new no.nav.aura.appconfig.Application();
        application.setName("app");
        application.getResources().add(createWebservice("myWs"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources(resourceElementFrom(myWS));
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(1, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("myWS")));
        ResourceReference storedReRef = newApplicationInstance.getResourceReferences().iterator().next();
        assertEquals(resReference.getID(), storedReRef.getID(), "Resource ref is the same");
    }

    @Test
    public void registerAppWithSameUsedResourceInNewRevisionShouldResourceReference() {
        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));
        applicationInstance.getResourceReferences().add(new ResourceReference(myWS, 123L));
        applicationInstance = repository.store(applicationInstance);
        assertEquals(1, applicationInstance.getResourceReferences().size(), "Has 1 used resources");
        ResourceReference resReference = applicationInstance.getResourceReferences().iterator().next();

        no.nav.aura.appconfig.Application application = new no.nav.aura.appconfig.Application();
        application.setName("app");
        application.getResources().add(createWebservice("myWs"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources(resourceElementFrom(myWS));
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(1, newApplicationInstance.getResourceReferences().size());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("myWS")));
        ResourceReference storedReRef = newApplicationInstance.getResourceReferences().iterator().next();
        assertEquals(resReference.getID(), storedReRef.getID(), "Resource ref is the same");
    }

    @Test
    public void registerAppWithMissingUsedResourceShouldDeleteResourceReference() {
        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));
        applicationInstance.getResourceReferences().add(new ResourceReference(myWS, null));
        applicationInstance = repository.store(applicationInstance);
        assertEquals(1, applicationInstance.getResourceReferences().size(), "Has 1 used resources");

        no.nav.aura.appconfig.Application application = new no.nav.aura.appconfig.Application();
        application.setName("app");

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources();
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(0, newApplicationInstance.getResourceReferences().size());
    }

    @Test
    public void renameResourceAndRegisterAppShouldUpdateReference() {
        Resource myWS = repository.store(new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));
        applicationInstance.getResourceReferences().add(new ResourceReference(myWS, 1L));
        applicationInstance = repository.store(applicationInstance);
        assertEquals(1, applicationInstance.getResourceReferences().size(), "Has 1 used resources");
        ResourceReference resReference = applicationInstance.getResourceReferences().iterator().next();
        assertEquals("myWS", resReference.getAlias());

        // remname WS
        myWS.setAlias("updatedWS");
        repository.store(myWS);

        no.nav.aura.appconfig.Application application = new no.nav.aura.appconfig.Application();
        application.setName("app");
        application.getResources().add(createWebservice("updatedWS"));

        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(application, "1.0");
        deployedApplication.addUsedResources(resourceElementFrom(myWS));
        service.registerDeployedApplication("test", "app", deployedApplication);

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertTrue(any(newApplicationInstance.getResourceReferences(), containsResource("updatedWs")));
        assertEquals(1, newApplicationInstance.getResourceReferences().size());
        ResourceReference storedReRef = newApplicationInstance.getResourceReferences().iterator().next();
        assertEquals(resReference.getID(), storedReRef.getID(), "Resource ref is the same");
        assertEquals("updatedWS", resReference.getAlias());
    }

    private Webservice createWebservice(String alias) {
        Webservice webservice = new Webservice();
        webservice.setAlias(alias);
        webservice.setMapToProperty("wsProp");
        return webservice;
    }

    private Predicate<ResourceReference> containsResource(final String alias) {
        return new Predicate<ResourceReference>() {
            public boolean apply(ResourceReference input) {
                return alias.equalsIgnoreCase(input.getAlias()) && !input.isFuture();
            }
        };
    }

    private Predicate<ResourceReference> containsFutureResource(final String alias) {
        return new Predicate<ResourceReference>() {
            public boolean apply(ResourceReference input) {
                return alias.equalsIgnoreCase(input.getAlias()) && input.isFuture();
            }
        };
    }

    private ResourceElement resourceElementFrom(Resource resource) {
        ResourceElement resourceElement = new ResourceElement(ResourceTypeDO.valueOf(resource.getType().name()), resource.getAlias());
        resourceElement.setId(resource.getID());
        return resourceElement;
    }

}
