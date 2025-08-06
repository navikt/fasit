package no.nav.aura.envconfig.model.infrastructure;

import com.google.common.collect.Sets;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.spring.SpringTest;
import no.nav.aura.envconfig.util.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.NoResultException;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ClusterTest extends SpringTest {

    private Environment environment;
    private Cluster cluster;

    @BeforeEach
    public void createEnvAndClusterWithNodesAndApplications() {
        environment = new Environment("hei", EnvironmentClass.u);
        cluster = new Cluster("mycluster", Domain.Devillo);
        cluster.addApplication(new Application("hei"));
        cluster.addApplication(new Application("hopp"));
        cluster.addNode(new Node("host.devillo.no", "root", "password"));
        cluster.addNode(new Node("post.devillo.no", "rot", "pass"));
        environment.addCluster(cluster);
        environment = repository.store(environment);
        cluster = TestHelper.assertAndGetSingle(environment.getClusters());
    }

    @Test
    public void removeApplicationByApplicationId_notFound() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            new Cluster("name", Domain.Devillo).removeApplicationByApplicationId(1L);
        });
    }

    @Test
    public void removeApplicationByApplicationId_found() {
        assertEquals(2, cluster.getApplicationInstances().size());
        ApplicationInstance applicationInstance = cluster.getApplicationInstances().iterator().next();
        cluster.removeApplicationByApplicationId(applicationInstance.getApplication().getID());
        assertEquals(1, cluster.getApplicationInstances().size());
        repository.store(environment);
        assertGetByIdReturnsNone(ApplicationInstance.class, applicationInstance.getID());
    }

    @Test
    public void removeNodeByNodeId_notFound() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            new Cluster("name", Domain.Devillo).removeNodeById(1L);
        });
    }

    @Test
    public void removeNodeByNodeId_found() {
        assertEquals(2, cluster.getNodes().size());
        cluster.removeNodeById(cluster.getNodes().iterator().next().getID());
        assertEquals(1, cluster.getNodes().size());
    }

    @Test
    public void addNodeWithDifferentDomain() {
        Cluster cluster = new Cluster("cluster", Domain.Devillo);
        cluster.addNode(new Node("a01.devillo.no", "user", "password"));
        cluster.addNode(new Node("b01.devillo-t.local", "", ""));
    }

    @Test
    public void getLoadBalancerUrl_notSet() {
        Cluster cluster = new Cluster("cluster", Domain.Devillo);
        assertNull(cluster.getLoadBalancerUrl());
    }

    @Test
    public void whenPlatformTypeIsNull_shouldDefaultToJboss() {
        Cluster cluster = new Cluster("cluster", Domain.Devillo);
        Node node = new Node("host.devillo.no", "user", "password");
        node.setPlatformType(null);
        cluster.addNode(node);
        assertThat(node.getPlatformType(), is(PlatformType.WILDFLY));
    }

    @Test
    public void registeringLoadBalancerUrlWithoutProtocol_shouldFail() throws Exception {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Cluster c = new Cluster("cluster", Domain.Devillo);
            c.setLoadBalancerUrl("mylb.adeo.no");
        });
    }

    private void assertGetByIdReturnsNone(Class<? extends ModelEntity> type, Long id) {
        try {
            repository.getById(type, id);
            Assertions.fail();
        } catch (NoResultException e) {
            // OK
        }
    }

    @Test
    public void correctUrlPatternIsAllowed() throws Exception {
        cluster.setLoadBalancerUrl("http://asdf.no");
        cluster.setLoadBalancerUrl("http://asdf.com:8989");
        cluster.setLoadBalancerUrl("https://asdf.com:8989");
        cluster.setLoadBalancerUrl("http://d34asdf.oera-t.com:8989");
        cluster.setLoadBalancerUrl("http://d34asdf.oera-t.com");
        cluster.setLoadBalancerUrl("http://d34asdf.oera-t.com/");
        cluster.setLoadBalancerUrl("http://d34asdf.oera-t.com/hei");
        cluster.setLoadBalancerUrl("http://d34asdf.oera-t.com:80/hei");
    }

    @Test
    public void invalidUrlFails() throws Exception {
        HashSet<String> invalidUrls = Sets.newHashSet("htttp://d34asdf.oera-t.com", "http;//d34asdf.oera-t.com", "htttp://d34asdf.oera-t.com", "http;//d34asdf.oera-t.com",
                "http:/d34asdf.oera_t.com", "http:/d34asdf.oera_t.com:90a90");

        for (String url : invalidUrls) {
            try {
                cluster.setLoadBalancerUrl(url);
            } catch (Exception e) {
                continue;
            }

            throw new AssertionError("Invalid URL " + url + " didn't fail");
        }
    }
}
