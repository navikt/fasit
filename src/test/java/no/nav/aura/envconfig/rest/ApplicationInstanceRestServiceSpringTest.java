package no.nav.aura.envconfig.rest;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.inject.Inject;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ApplicationInstanceRestServiceSpringTest extends SpringTest {

    private final String loadBalancer = "https://myloadbalancer.adeo.no";
    private ApplicationInstanceRestService service;
    private Environment env;
    private ApplicationInstance applicationInstance;
    private Cluster cluster;


    @BeforeEach
    public void setup() {
        service = new ApplicationInstanceRestService(repository, mock(SensuClient.class), mock(FasitKafkaProducer.class));
        env = new Environment("env", EnvironmentClass.t);
        cluster = new Cluster("myCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl(loadBalancer);
        env.addCluster(cluster);
        cluster.addNode(new Node("hostname.test.local", "username", "password"));
        env = repository.store(env);
    }

    @Test
    public void registeringApplicationWithLBInfoAndLBIsDefined_shouldUpdateLBUrlOnCluster(){
        addLBApplication();
        repository.store(new Resource("bigip", ResourceType.LoadBalancer, new Scope(EnvironmentClass.t)));
        assertThat(repository.findEnvironmentBy(env.getName()).getClusters().iterator().next().getLoadBalancerUrl(), is(loadBalancer));

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-loadbalancer.xml"));
        DeployedApplicationDO container = new DeployedApplicationDO(application, "1.0");
        service.registerDeployedApplication(env.getName(), application.getName(), container);
        assertThat(repository.findEnvironmentBy(env.getName()).getClusters().iterator().next().getLoadBalancerUrl(), is("https://app-" + env.getName() + ".adeo.no"));

    }

    @Test
    public void registeringApplicationWithLBInfoAndLBIsNotDefined_shouldUpdateLBUrlOnCluster() throws Exception {
        addLBApplication();
        assertThat(repository.findEnvironmentBy(env.getName()).getClusters().iterator().next().getLoadBalancerUrl(), is(loadBalancer));

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-loadbalancer.xml"));
        DeployedApplicationDO container = new DeployedApplicationDO(application, "1.0");
        service.registerDeployedApplication(env.getName(), application.getName(), container);
        assertThat(repository.findEnvironmentBy(env.getName()).getClusters().iterator().next().getLoadBalancerUrl(), is(loadBalancer));
    }

    private void addLBApplication() {
        Application lbApp = new Application("lbApp", "app", "no.nav.app");
        env.findClusterByName(cluster.getName()).addApplication(lbApp);
        env = repository.store(env);
    }

    private void addAppApplication() {
        Application app = new Application("app", "app", "no.nav.app");
        env.findClusterByName(cluster.getName()).addApplication(app);
        env = repository.store(env);
    }

    private void addSelftestApplication() {
        Application app = new Application("selftestApp", "app", "no.nav.app");
        env.findClusterByName(cluster.getName()).addApplication(app);
        env = repository.store(env);
    }



    @Test
    public void registeringApplicationWithoutLBInfo_shouldNotUpdateLBUrlOnCluster() throws Exception {
        addAppApplication();
        assertThat(repository.findEnvironmentBy(env.getName()).getClusters().iterator().next().getLoadBalancerUrl(), is(loadBalancer));
        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config.xml"));
        DeployedApplicationDO container = new DeployedApplicationDO(application, "1.0");

        service.registerDeployedApplication(env.getName(), application.getName(), container);
        assertThat(repository.findEnvironmentBy(env.getName()).getClusters().iterator().next().getLoadBalancerUrl(), is(loadBalancer));
    }

    @Test
    public void registeringApplicationWithLBInfoInDev_shouldUpdateLBUrlOnClusterToANode() throws Exception {
        Environment environment = new Environment("u1", EnvironmentClass.u);
        Cluster cluster = new Cluster("myCluster", Domain.Devillo);
        cluster.addNode(new Node("hostname.devillo.no", "user", "password"));
        String lbUrl = "https://someloadbalancer.test.local";
        cluster.setLoadBalancerUrl(lbUrl);
        Application app = new Application("nonLbApp");
        cluster.addApplication(app);
        environment.addCluster(cluster);
        repository.store(environment);
        assertThat(repository.findEnvironmentBy("u1").getClusters().iterator().next().getLoadBalancerUrl(), is(lbUrl));

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-without-loadbalancer.xml"));
        DeployedApplicationDO container = new DeployedApplicationDO(application, "1.0");

        service.registerDeployedApplication("u1", app.getName(), container);
        assertThat(repository.findEnvironmentBy("u1").getClusters().iterator().next().getLoadBalancerUrl(), containsString("hostname.devillo.no"));
    }

    @Test
    public void registeringApplicationWithoutLBUrlOnClusterWithServicesToExposeInDev_shouldWork() throws Exception {
        Environment env = repository.store(new Environment("u1", EnvironmentClass.u));
        Cluster cluster = new Cluster("myCluster", Domain.Devillo);
        String hostname = "hostname.devillo.no";
        cluster.addNode(new Node(hostname, "username", "password"));
        Application app = new Application("serviceExposingAppWithoutLBInfo");
        cluster.addApplication(app);
        env.addCluster(cluster);
        repository.store(env);
        assertThat(cluster.getLoadBalancerUrl(), nullValue());
        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-exposed-service-c.xml"));
        DeployedApplicationDO container = new DeployedApplicationDO(application, "1.0");
        service.registerDeployedApplication("u1", app.getName(), container);
        assertThat(env.getClusters().iterator().next().getLoadBalancerUrl(), containsString(hostname));
    }

    @Test
    public void registeringApplicationWithOldSelftestsPagePath() throws Exception {
        addLBApplication();

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-with-loadbalancer.xml"));
        DeployedApplicationDO container = new DeployedApplicationDO(application, "1.0");

        service.registerDeployedApplication("env", application.getName(), container);

        Set<ApplicationInstance> applicationInstances = repository.findEnvironmentBy("env").getApplicationInstances();
        assertEquals(1, applicationInstances.size());
        applicationInstance = applicationInstances.iterator().next();
        assertEquals(applicationInstance.getSelftestPagePath(), "/testme");
    }

    @Test
    public void registeringApplicationWithBothSelftestsTag() throws Exception {
        addSelftestApplication();

        no.nav.aura.appconfig.Application application = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream("app-config-selftest.xml"));
        DeployedApplicationDO container = new DeployedApplicationDO(application, "1.0");

        service.registerDeployedApplication("env", application.getName(), container);

        Set<ApplicationInstance> applicationInstances = repository.findEnvironmentBy("env").getApplicationInstances();
        assertEquals(1, applicationInstances.size());
        applicationInstance = applicationInstances.iterator().next();
        assertEquals(applicationInstance.getSelftestPagePath(), "/human");
    }


}
