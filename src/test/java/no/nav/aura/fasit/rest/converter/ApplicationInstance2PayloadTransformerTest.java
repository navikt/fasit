package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.spring.SpringTest;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload.NodeRefPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ApplicationInstance2PayloadTransformerTest extends SpringTest {

    private ApplicationInstance2PayloadTransformer transformer;
    
    @Autowired
    private ApplicationInstanceRepository applicationInstanceRepository;
    
    private Environment testEnv;
    private Application testApp;

    @BeforeEach
    public void setup() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
        
        // Create and persist test environment and application
        testEnv = repository.store(new Environment("coolEnv", EnvironmentClass.t));
        testApp = repository.store(new Application("myApp", null, null));
        
        transformer = new ApplicationInstance2PayloadTransformer(URI.create("http://mocked.no"), applicationInstanceRepository);
    }
    
    @AfterEach
    public void tearDown() {
    	repository.delete(testApp);
    	repository.delete(testEnv);
    }

    @Test
    public void selfTestUrlForNodes() {
        // Create and persist test cluster with nodes
        Cluster cluster = new Cluster("aCluster", Domain.TestLocal);
        Node node1 = new Node("ahost.com", null, null, EnvironmentClass.t, PlatformType.JBOSS);
        Node node2 = new Node("anotherhost.com", null, null, EnvironmentClass.t, PlatformType.JBOSS);
        cluster.addNode(node1);
        cluster.addNode(node2);
        cluster.addApplication(testApp);
        testEnv.addCluster(cluster);
        testEnv.addNode(node1);
        testEnv.addNode(node2);
        testEnv = repository.store(testEnv);
        
        // Create and persist application instance
        ApplicationInstance instance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment(testApp.getName(), testEnv.getName());
        instance.setSelftestPagePath("/a/path/to/selftest");
        instance = repository.store(instance);
        
        ApplicationInstancePayload transform = transformer.apply(instance);

        assertThat(transform.selfTestUrls.size(), is(2));
        assertThat(transform.selfTestUrls.contains("https://ahost.com:8443/a/path/to/selftest"), is(true));
        assertThat(transform.selfTestUrls.contains("https://anotherhost.com:8443/a/path/to/selftest"), is(true));
    }

    @Test
    public void selfTestUrlForLoadBalancer() {
        // Create and persist test cluster with load balancer
        Cluster cluster = new Cluster("aCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl("https://loadbalanced.com/");
        ApplicationInstance appInstance = cluster.addApplication(testApp);
        testEnv.addCluster(cluster);
        testEnv = repository.store(testEnv);
        
        // Create and persist application instance
        ApplicationInstance instance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment(testApp.getName(), testEnv.getName());
        instance.setSelftestPagePath("/a/path/to/selftest");
//        
        instance = repository.store(instance);
        
        ApplicationInstancePayload transform = transformer.apply(instance);

        assertThat(transform.selfTestUrls.size(), is(1));
        assertThat(transform.selfTestUrls.contains("https://loadbalanced.com/a/path/to/selftest"), is(true));
    }

    @Test
    public void duplicateSelftestUrlsAreRemoved() {
        // Create and persist test cluster with load balancer and node with same hostname
        Cluster cluster = new Cluster("aCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl("https://ahost.com:8443");
        cluster.addNode(new Node("ahost.com", null, null, EnvironmentClass.t, PlatformType.JBOSS));
        cluster.addApplication(testApp);
        testEnv.addCluster(cluster);
        testEnv = repository.store(testEnv);
        
        // Create and persist application instance
        ApplicationInstance instance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment(testApp.getName(), testEnv.getName());
        instance.setSelftestPagePath("/a/path/to/selftest");
        instance = repository.store(instance);
        
        ApplicationInstancePayload transform = transformer.apply(instance);

        assertThat(transform.selfTestUrls.size(), is(1));
    }

    @Test
    public void noSelfTestPagePath() {
        // Create and persist test cluster with load balancer
        Cluster cluster = new Cluster("aCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl("https://loadbalanced.com/");
        cluster.addApplication(testApp);
        testEnv.addCluster(cluster);
        testEnv = repository.store(testEnv);
        
        ApplicationInstance instance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment(testApp.getName(), testEnv.getName());
        
        ApplicationInstancePayload transform = transformer.apply(instance);

        assertThat(transform.selfTestUrls.size(), is(0));
    }

    @Test
    public void transformsPortsToNodeRefs() {
        Set<Port> ports = Set.of(
            new Port("host1", 1, "https"), 
            new Port("host1", 2, "http"), 
            new Port("host2", 3, "https")
        );
        Set<NodeRefPayload> nodeRefPayloads = transformer.toNodeRefs(ports);
        assertThat("set contains two elements", nodeRefPayloads.size(), is(2));

        NodeRefPayload host1 = nodeRefPayloads.stream()
            .filter(n -> n.hostname.equalsIgnoreCase("host1"))
            .findFirst()
            .get();
        assertThat("host1 has two ports", host1.ports.size(), is(2));
    }

    @Test
    public void failsWithInvalidPortType() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Set<Port> ports = Set.of(new Port("host1", 1, "invalid_port_type"));
            transformer.toNodeRefs(ports);
        });
    }
}