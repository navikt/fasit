package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.client.LifeCycleStatusDO;
import no.nav.aura.envconfig.client.NodeDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringTest;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.integration.VeraRestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@EnableJpaRepositories(basePackageClasses = NodeRepository.class)
@WebAppConfiguration
public class NodesRestServiceSpringTest extends SpringTest {

    private NodesRestService nodeService;
    private Node node;
    private Resource resource;
    private Cluster cluster;
    @Inject
    private NodeRepository nodeRepository;

    private final VeraRestClient vera = mock(VeraRestClient.class);

    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Set up a mock request context for ServletUriComponentsBuilder
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(1337);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        nodeService = new NodesRestService(repository, nodeRepository, vera);

        Environment utvEnv = new Environment("myUtvEnv", EnvironmentClass.u);
        cluster = new Cluster("cluster", Domain.Devillo);
        utvEnv.addCluster(cluster);

        utvEnv.addNode(cluster, new Node("myNewHost.devillo.no", "username", "password"));
        repository.store(utvEnv);
        node = nodeRepository.findNodeByHostName("myNewHost.devillo.no");
        node.changeStatus(LifeCycleStatus.ALERTED);
        repository.store(node);

        resource = new Resource("dmgr", ResourceType.DeploymentManager, new Scope(EnvironmentClass.u));
        resource.putPropertyAndValidate("hostname", "dmgr.devillo.no");
        resource.putPropertyAndValidate("username", "user");
        resource.putSecretAndValidate("password", "secret");
        resource.changeStatus(LifeCycleStatus.STOPPED);
        resource = repository.store(resource);

    }
    
    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();

//        repository.delete(cluster);
//        repository.delete(repository.findNodeBy("myNewHost.devillo.no"));
        repository.delete(resource);
		repository.delete(repository.findEnvironmentBy("myUtvEnv"));
	}

    @Test
    public void deleteNodeWithNoCluster() {
        Environment env2 = new Environment("env2", EnvironmentClass.u);
        env2.addNode(new Node("nodeWithoutCluster.devillo.no", "username", "password"));
        repository.store(env2);
        nodeService.deleteNode("nodeWithoutCluster.devillo.no");
    }

    @Test
    public void deleteLastNodeInCluster_willCallVeraToRegisterUndeployedEvent() {
        Environment env = repository.getEnvironmentBy(node);
        Cluster cluster = env.getClusters().iterator().next();
        Application application = repository.store(new Application("app"));
        cluster.addApplication(application);
        repository.store(cluster);
        nodeService.deleteNode("myNewHost.devillo.no");

        ArgumentCaptor<String> applicationName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> environment = ArgumentCaptor.forClass(String.class);
        verify(vera).notifyVeraOfUndeployment(applicationName.capture(), environment.capture(), any(String.class));
        assertThat(environment.getValue(), is("myutvenv"));
        assertThat(applicationName.getValue(), is(application.getName()));
    }

    @Test
    public void stopNode() {
        NodeDO nodeDO = new NodeDO();
        nodeDO.setStatus(LifeCycleStatusDO.STOPPED);
        nodeService.updateNode("myNewHost.devillo.no", nodeDO);
        assertEquals(LifeCycleStatus.STOPPED, node.getLifeCycleStatus());
    }

    @Test
    public void startNode() {
        NodeDO nodeDO = new NodeDO();
        nodeDO.setStatus(LifeCycleStatusDO.STARTED);
        nodeService.updateNode("myNewHost.devillo.no", nodeDO);
        assertNull(node.getLifeCycleStatus());
    }

    @Test
    public void stopDMgr() {
        NodeDO nodeDO = new NodeDO();
        nodeDO.setStatus(LifeCycleStatusDO.STOPPED);
        nodeService.updateNode("dmgr.devillo.no", nodeDO);
        assertEquals(LifeCycleStatus.STOPPED, resource.getLifeCycleStatus());
    }

    @Test
    public void startDMgr() {
        NodeDO nodeDO = new NodeDO();
        nodeDO.setStatus(LifeCycleStatusDO.STARTED);
        nodeService.updateNode("dmgr.devillo.no", nodeDO);
        assertNull(resource.getLifeCycleStatus());
    }
}
