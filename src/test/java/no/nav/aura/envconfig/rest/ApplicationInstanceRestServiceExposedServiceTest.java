package no.nav.aura.envconfig.rest;

import com.google.common.collect.Lists;
import no.nav.aura.appconfig.exposed.ExposedEjb;
import no.nav.aura.appconfig.exposed.ExposedService;
import no.nav.aura.appconfig.exposed.ExposedSoap;
import no.nav.aura.appconfig.exposed.NetworkZone;
import no.nav.aura.envconfig.client.DeployedApplicationDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringTest;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.integration.FasitKafkaProducer;
import no.nav.aura.integration.VeraRestClient;
import no.nav.aura.sensu.SensuClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ApplicationInstanceRestServiceExposedServiceTest extends SpringTest {

    private final String loadBalancer = "https://myloadbalancer.adeo.no";
    private ApplicationInstanceRestService service;
    private Environment testEnv;
    private ApplicationInstance applicationInstance;

    @BeforeEach
    public void setup()  {
        service = new ApplicationInstanceRestService(repository, mock(SensuClient.class), mock(FasitKafkaProducer.class));
        testEnv = new Environment("test", EnvironmentClass.t);
        Cluster cluster = new Cluster("myCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl(loadBalancer);
        testEnv.addCluster(cluster);
        Application app = new Application("app", "app", "no.nav.app");
        cluster.addApplication(app);
        cluster.addNode(new Node("hostname.test.local", "username", "password"));
        testEnv.addCluster(cluster);
        testEnv = repository.store(testEnv);
        repository.store(new Resource("appUser", ResourceType.Credential, new Scope(EnvironmentClass.t)));

        repository.store(new Resource("appDb2", ResourceType.DataSource, new Scope(EnvironmentClass.t)));

        Set<ApplicationInstance> applicationInstances = repository.findEnvironmentBy("test").getApplicationInstances();
        assertEquals(1, applicationInstances.size());
        applicationInstance = applicationInstances.iterator().next();
        assertNull(applicationInstance.getVersion());
    }

    @Test
    public void registerExposedService() {

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-exposed-service.xml"));
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());

        Set<ExposedServiceReference> exposedServices = newApplicationInstance.getExposedServices();
        assertEquals(8, exposedServices.size());
        Resource exposed = findByName(exposedServices, "myService").getResource();
        assertEquals(ResourceType.WebserviceEndpoint, exposed.getType(), "Exposed type");
        assertEquals("myService", exposed.getAlias(), "Exposed name");
        assertEquals(loadBalancer + "/minurl/something", exposed.getProperties().get("endpointUrl"), "Exposed endpoint");
        assertEquals("http://maven.adeo.no/nexus/content/groups/public/no/nav/tjenester/fim/nav-fim-echo-tjenestespesifikasjon/0.0.1/nav-fim-echo-tjenestespesifikasjon-0.0.1.zip", exposed
                .getProperties().get("wsdlUrl"), "Exposed wsdl");
        assertEquals(EnvironmentClass.t, exposed.getScope().getEnvClass(), "scope class");
        assertEquals(Domain.TestLocal, exposed.getScope().getDomain(), "scope domain");
        assertEquals("test", exposed.getScope().getEnvironmentName(), "scope envionment");
        assertNull(exposed.getScope().getApplication(), "scope application");
    }

    @Test
    public void registerExposedSoap() {

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-exposed-service.xml"));
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        Set<ExposedServiceReference> exposedServices = newApplicationInstance.getExposedServices();
        assertEquals(8, exposedServices.size());

        Resource resource = findByName(exposedServices, "theSoapService").getResource();
        assertEquals(ResourceType.WebserviceEndpoint, resource.getType(), "Exposed type");
        assertEquals("theSoapService", resource.getAlias(), "Exposed name");
        assertEquals(loadBalancer + "/minurl/something", resource.getProperties().get("endpointUrl"), "Exposed endpoint");
        assertEquals(EnvironmentClass.t, resource.getScope().getEnvClass(), "scope class");
        assertEquals(Domain.TestLocal, resource.getScope().getDomain(), "scope domain");
        assertEquals("test", resource.getScope().getEnvironmentName(), "scope envionment");
        assertNull(resource.getScope().getApplication(), "scope application");
    }

    @Test
    public void registerExposedEjb() {

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-exposed-service.xml"));
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        Set<ExposedServiceReference> exposedServices = newApplicationInstance.getExposedServices();
        assertEquals(8, exposedServices.size());
        Resource exposed = findByName(exposedServices, "deployEJB").getResource();
        assertEquals(ResourceType.EJB, exposed.getType(), "Exposed type");
        assertEquals("deployEJB", exposed.getAlias(), "Exposed name");
        assertEquals("java:/ejb/no/nav/EJBHome", exposed.getProperties().get("jndi"), "Exposed name");
        assertEquals("no.nav.gsak.ejb.EjbHome", exposed.getProperties().get("beanHomeInterface"), "Exposed name");
        assertEquals("no.nav.gsak.ejb.Ejben", exposed.getProperties().get("beanComponentInterface"), "Exposed name");
        assertEquals(EnvironmentClass.t, exposed.getScope().getEnvClass(), "scope class");
        assertEquals(Domain.TestLocal, exposed.getScope().getDomain(), "scope domain");
        assertEquals("test", exposed.getScope().getEnvironmentName(), "scope envionment");
        assertNull(exposed.getScope().getApplication(), "scope application");
    }

    @Test
    public void registerExposedUrl() {

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-exposed-service.xml"));
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        Set<ExposedServiceReference> exposedServices = newApplicationInstance.getExposedServices();
        assertEquals(8, exposedServices.size());
        Resource exposed = findByName(exposedServices, "aWebLink").getResource();
        assertEquals(ResourceType.BaseUrl, exposed.getType(), "Exposed type");
        assertEquals("https://myloadbalancer.adeo.no/linkToAnotherApp", exposed.getProperties().get("url"), "Exposed name");
        assertEquals(EnvironmentClass.t, exposed.getScope().getEnvClass(), "scope class");
        assertEquals(Domain.TestLocal, exposed.getScope().getDomain(), "scope domain");
        assertEquals("test", exposed.getScope().getEnvironmentName(), "scope envionment");
        assertNull(exposed.getScope().getApplication(), "scope application");
    }

    @Test
    public void registerExposedRest() {

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-exposed-service.xml"));
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        Set<ExposedServiceReference> exposedServices = newApplicationInstance.getExposedServices();
        assertEquals(8, exposedServices.size());
        Resource exposed = findByName(exposedServices, "myRestService").getResource();
        assertEquals(ResourceType.RestService, exposed.getType(), "Exposed type");
        assertEquals("https://myloadbalancer.adeo.no/myService", exposed.getProperties().get("url"), "Exposed name");
        assertEquals(EnvironmentClass.t, exposed.getScope().getEnvClass(), "scope class");
        assertEquals(Domain.TestLocal, exposed.getScope().getDomain(), "scope domain");
        assertEquals("test", exposed.getScope().getEnvironmentName(), "scope envionment");
        assertNull(exposed.getScope().getApplication(), "scope application");
    }

    @Test
    public void registerUrlExposedToNetworkZoneAll() {

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-exposed-service.xml"));
        for (ExposedService exposedService : application.getExposedServices(ExposedService.class)) {
            exposedService.setExportToZones(Lists.newArrayList(NetworkZone.ALL));
        }

        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        Set<ExposedServiceReference> exposedServices = newApplicationInstance.getExposedServices();
        assertEquals(8, exposedServices.size());

        Resource exposed = findByName(exposedServices, "aWebLink").getResource();
        assertEquals(EnvironmentClass.t, exposed.getScope().getEnvClass(), "scope class");
        assertNull(exposed.getScope().getDomain(), "scope domain");
        assertEquals("test", exposed.getScope().getEnvironmentName(), "scope envionment");
        assertNull(exposed.getScope().getApplication(), "scope application");

    }

    @Test
    public void registerServiceExposedToSBS() {

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        for (ExposedService exposedService : application.getExposedServices(ExposedService.class)) {
            exposedService.setExportToZones(Lists.newArrayList(NetworkZone.SBS));
        }

        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        Set<ExposedServiceReference> exposedServices = newApplicationInstance.getExposedServices();
        assertEquals(2, exposedServices.size());
        for (ExposedServiceReference exposedServiceReference : exposedServices) {
            Resource exposed = exposedServiceReference.getResource();
            if (!exposed.getAlias().equals("yourService")) {
                assertEquals(EnvironmentClass.t, exposed.getScope().getEnvClass(), "scope class");
                assertNull(exposed.getScope().getDomain(), "scope domain");
                assertEquals("test", exposed.getScope().getEnvironmentName(), "scope envionment");
                assertNull(exposed.getScope().getApplication(), "scope application");
            }
        }
    }

    @Test
    public void registerApplicationAndRemoveNonUsedExposedService() {
        Cluster otherCluster = new Cluster("otherCluster", Domain.Devillo);
        Application otherApp = new Application("otherApp.devillo.no", "app", "no.nav.app");
        otherCluster.addApplication(otherApp);
        testEnv.addCluster(otherCluster);
        repository.store(testEnv);

        Resource nonDependentWebservice = new Resource("otherws", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u));
        applicationInstance.getExposedServices().add(new ExposedServiceReference(nonDependentWebservice, null));
        repository.store(applicationInstance);

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(2, newApplicationInstance.getExposedServices().size());
    }

    @Test
    public void deleteExistingExposedResources() {
        assertEquals(0, applicationInstance.getExposedServices().size(), "noExposedServices");

        Resource exposedWebservice = new Resource("myService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t));
        applicationInstance.getExposedServices().add(new ExposedServiceReference(exposedWebservice, null));
        repository.store(applicationInstance);

        Collection<Resource> resoucesInDBbefore = repository.findResourcesByExactAlias(new Scope(testEnv.getScope()), ResourceType.WebserviceEndpoint, "myService");
        System.out.println(resoucesInDBbefore);
        assertEquals(1, resoucesInDBbefore.size(), "# resources");

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        service.registerDeployedApplication("test", "app", new DeployedApplicationDO(application, "1.0"));

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(2, newApplicationInstance.getExposedServices().size());

        Collection<Resource> resourcesInDBafter = repository.findResourcesByExactAlias(new Scope(testEnv.getScope()), ResourceType.WebserviceEndpoint, "myService");
        assertEquals(1, resourcesInDBafter.size(), "resources deleted");
        Resource resourceInDb = resourcesInDBafter.iterator().next();
        assertEquals(exposedWebservice, resourceInDb, "resource is same object");
        assertEquals(exposedWebservice.getID(), resourceInDb.getID(), "resource has same database id");
    }

    @Test
    public void undeployDeletesExposedResources() {
        Resource exposedWebservice = new Resource("myService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t));
        applicationInstance.getExposedServices().add(new ExposedServiceReference(exposedWebservice, null));
        applicationInstance.setVersion("1.0");
        repository.store(applicationInstance);

        Collection<Resource> resoucesInDBbefore = repository.findResourcesByExactAlias(new Scope(testEnv.getScope()), ResourceType.WebserviceEndpoint, "myService");
        assertEquals(1, resoucesInDBbefore.size(), "# resources");

        service.undeployApplication("test", "app");

        ApplicationInstance newApplicationInstance = repository.getById(ApplicationInstance.class, applicationInstance.getID());
        assertEquals(0, newApplicationInstance.getExposedServices().size());
        assertNull(newApplicationInstance.getVersion(), "version");

    }

    @Test
    public void verifyingApplication() {
        Cluster otherCluster = new Cluster("otherCluster", Domain.Devillo);
        Application otherApp = new Application("otherApp", "app", "no.nav.app");
        otherCluster.addApplication(otherApp);
        testEnv.addCluster(otherCluster);
        repository.store(testEnv);

        Resource dependentWebservice = new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t));
        applicationInstance.getExposedServices().add(new ExposedServiceReference(dependentWebservice, null));
        repository.store(applicationInstance);

        Resource otherWebservice = new Resource("myWS", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t));
        ApplicationInstance otherAppInstance = otherCluster.getApplicationInstances().iterator().next();
        otherAppInstance.getResourceReferences().add(new ResourceReference(otherWebservice, null));
        repository.store(otherAppInstance);

        service.verifyApplicationInstance("test", "app", no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml")));

    }

    private <T extends Exception> void verifyAndAssertExeption(String environment, String appname, no.nav.aura.appconfig.Application application, Class<T> exceptionType, String expectedExeptionText) {
        try {
            service.verifyApplicationInstance(environment, appname, application);
            fail("Expected exeption");
        } catch (Exception e) {
            assertThat(e, Matchers.instanceOf(exceptionType));
            assertThat(e.getMessage(), Matchers.containsString(expectedExeptionText));
        }
    }

    @Test
    public void failOnVerifyingApplicationWithExposedServiceSameAliasSameScopeDifferentApplication() {
        Cluster otherCluster = new Cluster("otherCluster", Domain.Devillo);
        Application otherApp = new Application("otherApp", "otherApp", "no.nav.otherApp");
        otherCluster.addApplication(otherApp);
        testEnv.addCluster(otherCluster);
        repository.store(testEnv);

        Resource existingWebservice = new Resource("yourService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t).envName(testEnv.getName()).domain(Domain.TestLocal));
        ApplicationInstance instanceOtherApp = getApplicationInstanceWithName("test", otherApp);
        instanceOtherApp.getExposedServices().add(new ExposedServiceReference(existingWebservice, null));
        repository.store(instanceOtherApp);

        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));

        verifyAndAssertExeption(testEnv.getName(), newApplication.getName(), newApplication, BadRequestException.class, "yourService of type WebserviceEndpoint already exists ");
    }

    public void failOnVerifyingApplicationWithExposedEjbSameAliasSameScopeDifferentApplication() {
        Cluster otherCluster = new Cluster("otherCluster", Domain.Devillo);
        Application otherApp = new Application("otherApp", "otherApp", "no.nav.otherApp");
        otherCluster.addApplication(otherApp);
        testEnv.addCluster(otherCluster);
        repository.store(testEnv);

        Resource existingEjb = new Resource("yourEjb", ResourceType.EJB, new Scope(EnvironmentClass.t));
        ApplicationInstance instanceOtherApp = getApplicationInstanceWithName("test", otherApp);
        instanceOtherApp.getExposedServices().add(new ExposedServiceReference(existingEjb, null));
        repository.store(instanceOtherApp);

        no.nav.aura.appconfig.Application newApplication = new no.nav.aura.appconfig.Application();
        newApplication.setName("app");
        ExposedEjb ejb = new ExposedEjb();
        ejb.setName("yourEjb");
        newApplication.getExposedServices().add(ejb);

        verifyAndAssertExeption(testEnv.getName(), newApplication.getName(), newApplication, BadRequestException.class, "yourEjb of type EJB already exists ");
    }

    @Test
    public void failOnVerifyingApplicationWithExposedServiceSameAliasSameScopeNoApplication() {
        Resource existingWebservice = new Resource("yourService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t).envName(testEnv.getName()).domain(Domain.TestLocal));
        repository.store(existingWebservice);
        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        verifyAndAssertExeption(testEnv.getName(), newApplication.getName(), newApplication, BadRequestException.class, "A resource can not be registrered more than once in the same scope");
    }

    @Test
    public void verifyApplicationWithExposedServiceSameAliasMoreSpesificScopeNoApplication() {
        Application otherapp = repository.store(new Application("otherapp"));
        Resource existingWebservice = new Resource("yourService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t).envName(testEnv.getName()).application(otherapp));
        repository.store(existingWebservice);

        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        service.verifyApplicationInstance(testEnv.getName(), newApplication.getName(), newApplication);
    }

    @Test
    public void verifyingApplicationWithExposedServiceSameAliasSameScopeSameApplication() {
        Resource existingWebservice = new Resource("yourService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t).envName(testEnv.getName()).domain(Domain.TestLocal));
        applicationInstance.getExposedServices().add(new ExposedServiceReference(existingWebservice, null));
        repository.store(applicationInstance);

        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        service.verifyApplicationInstance(testEnv.getName(), newApplication.getName(), newApplication);
    }

    @Test
    public void verifyingApplicationWithExposedServiceSameAliasDifferentScopeDifferentApps() {
        Environment preprodEnv = new Environment("preprod", EnvironmentClass.q);
        Cluster otherCluster = new Cluster("otherCluster", Domain.PreProd);
        Application otherApp = new Application("otherApp", "otherApp", "no.nav.otherApp");
        otherCluster.addApplication(otherApp);
        preprodEnv.addCluster(otherCluster);
        repository.store(preprodEnv);

        Resource existingWebservice = new Resource("yourService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.q));
        ApplicationInstance instanceOtherApp = getApplicationInstanceWithName("preprod", otherApp);
        instanceOtherApp.getExposedServices().add(new ExposedServiceReference(existingWebservice, null));
        repository.store(instanceOtherApp);

        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        service.verifyApplicationInstance(testEnv.getName(), newApplication.getName(), newApplication);
    }

    @Test
    public void verifyingApplicationWithExposedServiceDifferentAliasSameScopeDifferentApplication() {
        Cluster otherCluster = new Cluster("otherCluster", Domain.Devillo);
        Application otherApp = new Application("otherApp", "otherApp", "no.nav.otherApp");
        otherCluster.addApplication(otherApp);
        testEnv.addCluster(otherCluster);
        repository.store(testEnv);

        Resource existingWebservice = new Resource("aNewService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t));
        ApplicationInstance instanceOtherApp = getApplicationInstanceWithName("test", otherApp);
        instanceOtherApp.getExposedServices().add(new ExposedServiceReference(existingWebservice, null));
        repository.store(instanceOtherApp);

        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));

        service.verifyApplicationInstance(testEnv.getName(), newApplication.getName(), newApplication);

    }

    @Test
    public void verifyApplicationWithExposedServiceSameAliasNoEnvironementScopeNoApplication() {
        Resource existingWebservice = new Resource("yourService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t));
        repository.store(existingWebservice);

        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        service.verifyApplicationInstance(testEnv.getName(), newApplication.getName(), newApplication);
    }

    @Test
    public void verifyApplicationWithExposedServiceSameAliasNoDomainScopeNoApplication() {
        Resource existingWebservice = new Resource("yourService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t).envName(testEnv.getName()));
        repository.store(existingWebservice);

        no.nav.aura.appconfig.Application newApplication = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        service.verifyApplicationInstance(testEnv.getName(), newApplication.getName(), newApplication);
    }

    @Test
    public void failOnVerifyingApplicationWithExposedServiceSameAliasNoDomainDifferenetApplication() {
        Application otherApp = repository.store(new Application("otherApp", "otherApp", "no.nav.otherApp"));
        ApplicationInstance otherAppInstance = testEnv.getClusters().iterator().next().addApplication(otherApp);
        repository.store(testEnv);

        Resource existingWebservice = new Resource("yourService", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.t).envName(testEnv.getName()));
        otherAppInstance.getExposedServices().add(new ExposedServiceReference(existingWebservice, null));
        repository.store(otherAppInstance);

        no.nav.aura.appconfig.Application newApplication = new no.nav.aura.appconfig.Application();
        newApplication.setName("app");
        ExposedSoap exposed = new ExposedSoap();
        exposed.setName("yourService");
        exposed.setExportToZones(Arrays.asList(NetworkZone.ALL));
        newApplication.getExposedServices().add(exposed);
        verifyAndAssertExeption(testEnv.getName(), newApplication.getName(), newApplication, BadRequestException.class, "already exists in Fasit and is not exposed by application ");

        // ok with other zone
        exposed.setExportToZones(new ArrayList<NetworkZone>());
        service.verifyApplicationInstance(testEnv.getName(), newApplication.getName(), newApplication);
    }

    private ApplicationInstance getApplicationInstanceWithName(String envName, Application otherApp) {
        Set<ApplicationInstance> applicationInstances = repository.findEnvironmentBy(envName).getApplicationInstances();
        for (ApplicationInstance instance : applicationInstances) {
            if (instance.getApplication().getName().equals(otherApp.getName())) {
                return instance;
            }
        }
        return null;
    }

    private <T extends ExposedServiceReference> T findByName(Collection<T> exposedServices, String name) {
        for (T exposedService : exposedServices) {
            if (name.equals(exposedService.getName())) {
                return exposedService;
            }
        }
        Assertions.fail("Exposed service with name " + name + " not found");
        return null;
    }

}
