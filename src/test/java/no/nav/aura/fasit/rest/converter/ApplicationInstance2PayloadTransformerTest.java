package no.nav.aura.fasit.rest.converter;

import com.google.common.collect.Sets;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload.NodeRefPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ApplicationInstance2PayloadTransformerTest {

    private ApplicationInstance2PayloadTransformer transformer = new ApplicationInstance2PayloadTransformer(null, null);

    @BeforeEach
    public void setup() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
        ApplicationInstanceRepository repoMock = Mockito.mock(ApplicationInstanceRepository.class);
        when(repoMock.findEnvironmentWith(any(ApplicationInstance.class))).thenReturn(new Environment("coolEnv", EnvironmentClass.t));
        transformer = new ApplicationInstance2PayloadTransformer(URI.create("http://mocked.no"), repoMock);

    }

    @Test
    public void selfTestUrlForNodes() {
        Cluster cluster = new Cluster("aCluster", Domain.TestLocal);
        cluster.addNode(new Node("ahost.com", null, null, EnvironmentClass.t, PlatformType.JBOSS));
        cluster.addNode(new Node("anotherhost.com", null, null, EnvironmentClass.t, PlatformType.JBOSS));
        ApplicationInstance instance = new ApplicationInstance(new Application("myApp", null, null), cluster);
        instance.setSelftestPagePath("/a/path/to/selftest");
        instance.setID(69L);
        ApplicationInstancePayload transform = transformer.apply(instance);

        assertThat(transform.selfTestUrls.size(), is(2));
        assertThat(transform.selfTestUrls.contains("https://ahost.com:8443/a/path/to/selftest"), is(true));
        assertThat(transform.selfTestUrls.contains("https://anotherhost.com:8443/a/path/to/selftest"), is(true));
    }

    @Test
    public void selfTestUrlForLoadBalancer() {
        Cluster cluster = new Cluster("aCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl("https://loadbalanced.com/");
        ApplicationInstance instance = new ApplicationInstance(new Application("myApp", null, null), cluster);
        instance.setSelftestPagePath("/a/path/to/selftest");
        instance.setID(69L);
        ApplicationInstancePayload transform = transformer.apply(instance);

        assertThat(transform.selfTestUrls.size(), is(1));
        assertThat(transform.selfTestUrls.contains("https://loadbalanced.com/a/path/to/selftest"), is(true));
    }

    @Test
    public void duplicateSelftestUrlsAreRemoved() {
        Cluster cluster = new Cluster("aCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl("https://ahost.com:8443");
        cluster.addNode(new Node("ahost.com", null, null, EnvironmentClass.t, PlatformType.JBOSS));
        ApplicationInstance instance = new ApplicationInstance(new Application("myApp", null, null), cluster);
        instance.setSelftestPagePath("/a/path/to/selftest");
        instance.setID(69L);
        ApplicationInstancePayload transform = transformer.apply(instance);

        assertThat(transform.selfTestUrls.size(), is(1));
    }

    @Test
    public void noSelfTestPagePath() {
        Cluster cluster = new Cluster("aCluster", Domain.TestLocal);
        cluster.setLoadBalancerUrl("https://loadbalanced.com/");
        ApplicationInstance instance = new ApplicationInstance(new Application("myApp", null, null), cluster);
        instance.setID(69L);
        ApplicationInstancePayload transform = transformer.apply(instance);

        assertThat(transform.selfTestUrls.size(), is(0));
    }


    @Test
    public void transformsPortsToNodeRefs() {
        Set<Port> ports = Sets.newHashSet(new Port("host1", 1, "https"), new Port("host1", 2, "http"), new Port("host2", 3, "https"));
        Set<NodeRefPayload> nodeRefPayloads = transformer.toNodeRefs(ports);
        assertThat("set contains two elements", nodeRefPayloads.size(), is(2));

        NodeRefPayload host1 = nodeRefPayloads.stream().filter(n -> n.hostname.equalsIgnoreCase("host1")).findFirst().get();
        assertThat("host1 has two ports", host1.ports.size(), is(2));
    }

    @Test
    public void failsWithInvalidPortType() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Set<Port> ports = Sets.newHashSet(new Port("host1", 1, "invalid_port_type"));
            transformer.toNodeRefs(ports);
        });
    }



}