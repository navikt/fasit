package no.nav.aura.fasit.repository;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;
import no.nav.aura.envconfig.util.TestHelper;
import no.nav.aura.fasit.repository.specs.NodeSpecs;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import jakarta.inject.Inject;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringJUnitConfig(classes = {SpringUnitTestConfig.class})
@Transactional
public class NodeRepositoryTest {

    @Inject
    NodeRepository nodeRepository;

    @Autowired
    private FasitRepository repository;
    private Application application;
    private Environment t1;
    private Cluster clusterTestLocal;
    private Node nodeTestLocal;

    private ApplicationGroup appGroupWithOneApplication;
    private ApplicationGroup multiApplicationGroup;

    @BeforeEach
    public void setup() throws Exception {
        String appName = "tsys";

        application = repository.store(new Application(appName));
        appGroupWithOneApplication = repository.store(new ApplicationGroup("appGroupWithOneApplication", application));

        multiApplicationGroup = repository.store(new ApplicationGroup("appGroupWithMultipleApplications"));
        Application firstapp = createApplication("myFirstApp");
        multiApplicationGroup.addApplication(firstapp);
        multiApplicationGroup.addApplication(createApplication("mySecondApp"));
        multiApplicationGroup.addApplication(createApplication("myThirdApp"));

        repository.store(multiApplicationGroup);

        t1 = new Environment("t1", EnvironmentClass.t);
        clusterTestLocal = new Cluster(appName, Domain.TestLocal);
        t1.addCluster(clusterTestLocal);
        clusterTestLocal.addApplication(application);
        nodeTestLocal = new Node("nOde1.test.local", "deployer", "", t1.getEnvClass(), PlatformType.JBOSS);
        t1.addNode(clusterTestLocal, nodeTestLocal);

        Cluster clusterOeraT = new Cluster(appName, Domain.OeraT);
        Node node_1 = new Node("a1234.oerat.no", "srvyo", "s45oo1xxs1Z", t1.getEnvClass(), PlatformType.JBOSS);
        clusterOeraT.addNode(node_1);
        clusterOeraT.addApplication(application);
        clusterOeraT.addApplication(firstapp);

        t1 = repository.store(t1);
        clusterTestLocal = TestHelper.assertAndGetSingle(t1.getClusters());
        nodeTestLocal = TestHelper.assertAndGetSingle(t1.getNodes());
        repository.store(clusterOeraT);
    }
    
    @AfterEach
    public void tearDown() throws Exception {
		// Cleanup the repository after each test
		repository.delete(t1);
		repository.delete(application);
		repository.delete(appGroupWithOneApplication);
		repository.delete(multiApplicationGroup);
	}
    

    private Application createApplication(String applicationName) {
        Application entity = new Application(applicationName);
        return (Application) repository.store(entity);
    }

    @Test
    public void findClusterByNode() {
        Cluster cluster = nodeRepository.findClusterByNode(nodeTestLocal);
        assertEquals(clusterTestLocal, cluster);
    }

    @Test
    public void findNodeByHostName() {
        assertEquals(nodeTestLocal, nodeRepository.findNodeByHostName(nodeTestLocal.getHostname()));
        assertEquals(nodeTestLocal, nodeRepository.findNodeByHostName(nodeTestLocal.getHostname().toUpperCase()));
        assertEquals(nodeTestLocal, nodeRepository.findNodeByHostName(nodeTestLocal.getHostname().toLowerCase()));
        assertNull(nodeRepository.findNodeByHostName("unknownhost"));
    }

    @Test
    public void findNodeByLikeHostName() {
        assertThat(nodeRepository.findByHostnameContainingIgnoreCase("nOde1.test.local"), Matchers.hasItem(nodeTestLocal));
        // assertEquals(nodeTestLocal, nodeRepository.findByHostnameContainingIgnoreCase("node1"));
        // assertEquals(nodeTestLocal, nodeRepository.findByHostnameContainingIgnoreCase("test.local"));
        assertThat(nodeRepository.findByHostnameContainingIgnoreCase("unknownhost"), Matchers.empty());
    }

    @Test
    public void findEnvByNode() {
        Environment env = nodeRepository.findEnvironment(nodeTestLocal);
        assertEquals(t1, env);
    }

    @Test
    public void findNodesinEnvironment() {
        List<Node> nodes = nodeRepository.findNodesByEnvironmentName("t1");
        assertEquals(1, nodes.size());
    }

    @Test
    public void findNoNodesinEnvironment() {
        List<Node> nodes = nodeRepository.findNodesByEnvironmentName("t2");
        assertEquals(0, nodes.size());
    }

    @Test
    public void findNodesinEnvironmentClass() {
        List<Node> nodes = nodeRepository.findNodesByEnvironmentClass(EnvironmentClass.t);
        assertEquals(1, nodes.size());
    }

    @Test
    public void findNoNodesinEnvironmentClass() {
        List<Node> nodes = nodeRepository.findNodesByEnvironmentClass(EnvironmentClass.q);
        assertEquals(0, nodes.size());
    }

    @Test
    public void findNodesinEnvironmentClassSpec() {
        assertThat("t ", nodeRepository.findAll(NodeSpecs.findByEnvironmentClass(EnvironmentClass.t)), hasSize(1));
        assertThat("q ", nodeRepository.findAll(NodeSpecs.findByEnvironmentClass(EnvironmentClass.q)), hasSize(0));

    }

    @Test
    public void findNodesinEnvironmentSpec() {
        assertThat("t ", nodeRepository.findAll(NodeSpecs.findByEnvironment("t1")), hasSize(1));
        assertThat("q ", nodeRepository.findAll(NodeSpecs.findByEnvironment("t8")), hasSize(0));

    }

    @Test
    public void findNodesByTypeSpec() {
        assertThat("jboss", nodeRepository.findAll(NodeSpecs.findByType(PlatformType.JBOSS)), hasSize(2));
        assertThat("bpm", nodeRepository.findAll(NodeSpecs.findByType(PlatformType.BPM)), hasSize(0));
    }

    @Test
    public void findNodesByHostnameSpec() {
        String hostname = nodeTestLocal.getHostname();
        assertEquals(nodeTestLocal, nodeRepository.findOne(NodeSpecs.findByLikeHostname(hostname)).get(), "equal");
        assertEquals(nodeTestLocal, nodeRepository.findOne(NodeSpecs.findByLikeHostname(hostname.toLowerCase())).get(), "lower");
        assertEquals(nodeTestLocal, nodeRepository.findOne(NodeSpecs.findByLikeHostname(hostname.toUpperCase())).get(), "upper");

        assertEquals(nodeTestLocal, nodeRepository.findOne(NodeSpecs.findByLikeHostname("de1")).get(), "like");
        assertNull(nodeRepository.findOne(NodeSpecs.findByLikeHostname("tullogtøys")).orElse(null));

    }

    @Test
//    @Disabled
    public void findNodesByApplicationSpec() {
        assertThat("existing app", nodeRepository.findAll(NodeSpecs.findByApplication(application.getName())), hasSize(2));
        assertThat("existing app ignorecase", nodeRepository.findAll(NodeSpecs.findByApplication("tSYs")), hasSize(2));
        assertThat("existing app 2", nodeRepository.findAll(NodeSpecs.findByApplication("myfirstapp")), hasSize(1));
        assertThat("unknown app", nodeRepository.findAll(NodeSpecs.findByApplication("unknown")), empty());
    }
    
    @Test
    public void findNodesByApplication() {
        List<Node> findNodeByApplication = nodeRepository.findNodeByApplication(application.getName());
        assertThat("existing app", findNodeByApplication, hasSize(2));
        assertThat("existing app ignorecase", nodeRepository.findNodeByApplication("tSYs"), hasSize(2));
        assertThat("existing 2", nodeRepository.findNodeByApplication("myFirstApp"), hasSize(1));
        
        assertThat("unknown app", nodeRepository.findNodeByApplication("unknown"), empty());
    }
}
