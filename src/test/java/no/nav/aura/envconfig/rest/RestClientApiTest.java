package no.nav.aura.envconfig.rest;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.client.*;
import no.nav.aura.envconfig.client.DomainDO.EnvClass;
import no.nav.aura.envconfig.client.rest.PropertyElement;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.fasit.client.model.RegisterApplicationInstancePayload;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class RestClientApiTest extends RestTest {

    private FasitRestClient client;
    private static Application app;
    private static Environment appGroupEnv;
    private static Environment env;
    private static ApplicationGroup appGroup;
    private static Environment newEnv;

    @BeforeAll
    public static void setUpData() {
        env = new Environment("myTestEnv", EnvironmentClass.u);
        Cluster cluster = new Cluster("cluster", Domain.Devillo);
        cluster.setLoadBalancerUrl("https://lb.adeo.no");
        env.addCluster(cluster);
        app = (Application) repository.store(new Application("myApp", "myApp-appconfig", "no.nav.myApp"));
        cluster.addApplication(app);
        Node node = new Node("myNewHost.devillo.no", "username", "pass");
        env.addNode(cluster, node);
        cluster.addNode(node);
        repository.store(env);

        Resource db = new Resource("myDB", no.nav.aura.envconfig.model.resource.ResourceType.DataSource, env.getScope().domain(Domain.Devillo));
        db.putPropertyAndValidate("url", "jdbc:url");
        db.putPropertyAndValidate("username", "user");
        db.putPropertyAndValidate("oemEndpoint", "test");
        db.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        db.putSecretAndValidate("password", "secret");
        repository.store(db);

        appGroup = repository.store(new ApplicationGroup("myAppGrp"));
        Application firstApp = repository.store(new Application("firstApp"));
        Application secondApp = repository.store(new Application("mySecondApp"));
        appGroup.addApplication(firstApp);
        appGroup.addApplication(secondApp);
        repository.store(appGroup);

        appGroupEnv = repository.store(new Environment("myenv", EnvironmentClass.u));
        Cluster appGroupcluster = new Cluster("myCluster", Domain.Devillo);
        appGroupcluster.addApplicationGroup(appGroup);
        appGroupEnv.addCluster(appGroupcluster);
        repository.store(appGroupEnv);

        newEnv = repository.store(new Environment("newAppGrpEnv", EnvironmentClass.u));

    }

    @BeforeEach
    public void setupRestClient() {
        client = new FasitRestClient("http://localhost:" + jetty.getPort() + "/conf", "prodadmin", "prodadmin");
    }

//    @AfterEach
//    public void resteasy_should_leave_them_entities_alone() {
//        client.getEnvironments();
//    }
//
//    @AfterEach
//    public void checkThatTheHttpClientIsLeftInAUsableState() {
//        client.getApplicationInstance("myTestEnv", "myApp");
//    }

    @Test
    public void testGetCluster() {
        ApplicationDO appInfo = client.getApplicationInfo("myApp");
        assertEquals("myApp-appconfig", appInfo.getAppConfigArtifactId());
        assertEquals("no.nav.myApp", appInfo.getAppConfigGroupId());
        ApplicationInstanceDO appInstance = client.getApplicationInstance("myTestEnv", "myApp");
        ClusterDO cluster = appInstance.getCluster();
        assertThat(cluster.getNodes().length, greaterThanOrEqualTo(1));
        assertThat(cluster.getDomainDO().getFqn(), equalTo("devillo.no"));
        assertThat(getHostnames(cluster.getNodesAsList()), hasItem("myNewHost.devillo.no"));
    }

    @Test
    public void testGetResource() {
        ResourceElement resource = client.getResource("myTestEnv", "myDB", ResourceTypeDO.DataSource, DomainDO.Devillo, "myApp");
        assertEquals("myDB", resource.getAlias());
        assertEquals(DomainDO.Devillo, resource.getDomain());
        assertEquals("u", resource.getEnvironmentClass());
        assertEquals(ResourceTypeDO.DataSource, resource.getType());
        assertEquals(5, resource.getProperties().size());
    }

    @Test
    public void testFindResources() {
        Collection<ResourceElement> resources = client.findResources(EnvClass.u, "myTestEnv", DomainDO.Devillo, "myApp", ResourceTypeDO.DataSource, "myDB");
        assertEquals(1, resources.size(), "all params");
        resources = client.findResources(EnvClass.u, null, null, null, null, "myDB");
        assertEquals(1, resources.size(), "with nulls");
        resources = client.findResources(EnvClass.u, null, DomainDO.Adeo, null, null, "myDB");
        assertEquals(0, resources.size(), "Not found");
    }

    @Test
    public void testResourceExists() {
        assertTrue(client.resourceExists(EnvClass.u, "myTestEnv", DomainDO.Devillo, "myApp", ResourceTypeDO.DataSource, "myDB"), "all params");
        assertFalse(client.resourceExists(EnvClass.u, "otherEnv", DomainDO.Devillo, "myApp", ResourceTypeDO.DataSource, "myDB"), "Not found");
    }

    @Test
    public void registerApplicationWithComment() {
        RegisterApplicationInstancePayload payload = new RegisterApplicationInstancePayload(app.getName(), "1.0.2", env.getName());
        payload.setNodes(Arrays.asList("myNewHost.devillo.no"));
        client.setOnBehalfOf("otheruser");
        client.registerApplication(payload, "my new comment");

        List<ApplicationInstance> appInstances = repository.findApplicationInstancesBy(app);
        assertEquals(1, appInstances.size());
        ApplicationInstance instance = appInstances.get(0);
        assertEquals("myApp", instance.getApplication().getName());
        assertEquals("1.0.2", instance.getVersion());
        FasitRevision<ApplicationInstance> headrevision = getHeadrevision(instance);
        assertEquals("my new comment", headrevision.getMessage());
        assertEquals("prodadmin", headrevision.getAuthor());
        assertEquals("otheruser", headrevision.getOnbehalfOf().getId());
    }

    @Test
    public void should_delete_node_based_on_hostname() throws Exception {
        String hostname = "tullball.devillo.no";
        NodeDO nodeDO = client.registerNode(createNodeDO(hostname), "Told you so");
        assertNotNull(nodeDO.getRef());
        final Node node = repository.findNodeBy(hostname);
        assertNotNull(node);
        assertLastRevisionMessage(node, "Told you so");

        client.deleteNode(hostname, "Yeah; right");

        assertNull(repository.findNodeBy(hostname));
        assertLastRevisionMessage(node, "Yeah; right");
    }

    @Test
    public void should_throw_exception_when_trying_to_delete_nonexisting_node() throws URISyntaxException {
        Assertions.assertThrows(RuntimeException.class, () -> {
            client.deleteNode("nonexisting.devillo.no", "Yeah; right");
        });
    }

    @Test
    public void whenIncompleteDataWhenRegisteringNode_shouldGetException() throws URISyntaxException {
        NodeDO noDamain = createNodeDO("myNode.devillo.no");
        noDamain.setDomain(null);
        NodeDO noEnvironment = createNodeDO("myNode.devillo.no");
        noEnvironment.setEnvironmentName(null);
        NodeDO emptyApplication = createNodeDO("myNode.devillo.no");
        assertThatExceptionIsThrown(noDamain);
        assertThatExceptionIsThrown(noEnvironment);
        assertThatExceptionIsThrown(emptyApplication);
    }

    @Test
    public void addNodeToExistingCluster() throws URISyntaxException {
        NodeDO node = createNodeDO("mysecondhost.devillo.no", "mytestenv", "myApp");
        client.registerNode(node, "comment");
        ApplicationInstanceDO applicationInstance = client.getApplicationInstance("mytestenv", "myApp");
        ClusterDO storedCluster = applicationInstance.getCluster();
        assertThat(storedCluster.getNodesAsList(), hasSize(2));
        assertThat(getHostnames(storedCluster.getNodesAsList()), hasItem("mysecondhost.devillo.no"));
    }

    @Test
    @Transactional
    public void whenDeletingLastNodeInCluster_theClusterWillAlsoBeDeleted() throws URISyntaxException {
        Environment environment = repository.store(new Environment("newEnv", EnvironmentClass.u));
        NodeDO node = createNodeDO("willSoonBeDeleted.devillo.no", environment.getName(), "myApp");
        client.registerNode(node, "comment");
        assertThat(client.getApplicationInstance("newEnv", "myApp").getCluster(), notNullValue());
        client.deleteNode("willSoonBeDeleted.devillo.no", null);
        assertThat(client.getApplicationInstances("newEnv"), hasSize(0));
    }

    @Test
    public void stopNode() throws URISyntaxException {
        NodeDO node = new NodeDO();
        node.setHostname("myNewHost.devillo.no");
        node.setStatus(LifeCycleStatusDO.STOPPED);
        client.setOnBehalfOf("otheruser");
        client.updateNode(node, "Stopping it");
        Node storedNode = repository.findNodeBy("myNewHost.devillo.no");
        assertEquals(LifeCycleStatus.STOPPED, storedNode.getLifeCycleStatus());
        FasitRevision<Node> headrevision = getHeadrevision(storedNode);
        assertEquals("Stopping it", headrevision.getMessage());
        assertEquals("prodadmin", headrevision.getAuthor());
        assertEquals("otheruser", headrevision.getOnbehalfOf().getId());
    }

    @Test
    public void addNodeToExistingClusterUsingApplicationGroup() throws URISyntaxException {
        client.registerNode(createNodeDO("secondhost.devillo.no", appGroupEnv.getName(), appGroup.getName()), "myComment");
        assertThatAllApplicationsInApplicationGroupsIsMappedToCluster("secondhost.devillo.no", appGroupEnv, appGroup);
    }

    @Test
    public void createNewClusterForApplicationGroup() throws URISyntaxException {
        client.registerNode(createNodeDO("newappgrphost.devillo.no", newEnv.getName(), appGroup.getName()), "myComment");
        assertThatAllApplicationsInApplicationGroupsIsMappedToCluster("newappgrphost.devillo.no", newEnv, appGroup);
    }

    private void assertThatAllApplicationsInApplicationGroupsIsMappedToCluster(String hostname, Environment env, ApplicationGroup appGrp) {
        for (Application application : appGrp.getApplications()) {
            ApplicationInstanceDO appInstance = client.getApplicationInstance(env.getName(), application.getName());
            assertThat(getHostnames(appInstance.getCluster().getNodesAsList()), hasItem(hostname));
        }
    }

    private void assertThatExceptionIsThrown(NodeDO node) {
        try {
            client.registerNode(node, "this should fail");
        } catch (RuntimeException re) {
            assertThat(re.getMessage(), containsString("Domain, environmentName and applicationMappingName mandatory"));
        }
    }

    private <T extends ModelEntity> void assertLastRevisionMessage(final T entity, final String expected) {
        FasitRevision<T> headrevision = getHeadrevision(entity);
        assertEquals(expected, headrevision.getMessage());
    }

    private List<String> getHostnames(final List<NodeDO> nodes) {
        return Lists.transform(nodes, new Function<NodeDO, String>() {
            public String apply(@Nullable NodeDO input) {
                return input.getHostname();
            }
        });
    }

    private NodeDO createNodeDO(String hostname) throws URISyntaxException {
        return createNodeDO(hostname, "mytestenv", "myApp");
    }

    private NodeDO createNodeDO(String hostname, String environmentName, String applicationMapping) throws URISyntaxException {
        NodeDO nodeDO = new NodeDO();
        nodeDO.setDomain("devillo.no");
        nodeDO.setEnvironmentClass(EnvironmentClass.u.name());
        nodeDO.setEnvironmentName(environmentName);
        nodeDO.setPlatformType(PlatformTypeDO.JBOSS);
        nodeDO.setApplicationMappingName(applicationMapping);
        nodeDO.setHostname(hostname);
        return nodeDO;
    }

    @Test
    public void testRegisterResourceWithNoScope() {
        client.registerResource(createResourceElement("mintjeneste"), "registert tjeneste");
        ResourceElement resource = client.getResource("mytestenv", "mintjeneste", ResourceTypeDO.BaseUrl, DomainDO.Devillo, "myApp");
        assertEquals("mintjeneste", resource.getAlias());
        assertNotNull(resource);
        assertNull(resource.getEnvironmentName());
        assertEquals("u", resource.getEnvironmentClass());
        assertNull(resource.getDomain());
        assertNull(resource.getApplication());
        assertLastRevisionMessage(repository.getById(Resource.class, resource.getId()), "registert tjeneste");
    }

    @Test
    public void updateResource() {
        Resource newResource = new Resource("updateMe", no.nav.aura.envconfig.model.resource.ResourceType.DataSource, new Scope(EnvironmentClass.u));
        newResource.putPropertyAndValidate("url", "http://someurl");
        newResource.putPropertyAndValidate("username", "unchanged");
        newResource.putPropertyAndValidate("oemEndpoint", "unchanged");
        newResource.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        newResource.putSecretAndValidate("password", "password");
        Resource resource = repository.store(newResource);
 
        ResourceElement updateThis = new ResourceElement(ResourceTypeDO.DataSource, "updateMe");
        updateThis.addProperty(new PropertyElement("url", "http://der/her"));
        updateThis.setEnvironmentName(env.getName());
        updateThis.setDomain(DomainDO.Devillo);
        updateThis.setApplication(app.getName());
        updateThis.setLifeCycleStatus(LifeCycleStatusDO.STOPPED);
        updateThis.setAccessAdGroup("accessGroup");
        
        ResourceElement updatedResource = client.updateResource(resource.getID(), updateThis, "oppdatere denne her");
        assertEquals("updateMe", updatedResource.getAlias());
        assertEquals("http://der/her", updatedResource.getPropertyString("url"));
        assertEquals("unchanged", updatedResource.getPropertyString("username"));
        assertEquals(LifeCycleStatusDO.STOPPED, updatedResource.getLifeCycleStatus());
        assertEquals("accessGroup", updatedResource.getAccessAdGroup());
        
        assertEquals(DomainDO.Devillo, updatedResource.getDomain());
        assertEquals(env.getName(), updatedResource.getEnvironmentName());
        assertEquals(app.getName(), updatedResource.getApplication());
        assertLastRevisionMessage(repository.getById(Resource.class, resource.getID()), "oppdatere denne her");
    }

    @Test
    public void testRegisterResourceWithFullScope() {
        ResourceElement newResource = createResourceElement("fullScopeResource");
        newResource.setEnvironmentClass("u");
        newResource.setEnvironmentName("myTestEnv");
        newResource.setDomain(DomainDO.Devillo);
        newResource.setApplication("myApp");
        newResource.addProperty(new PropertyElement("url", "http://der/her"));
        client.registerResource(newResource, "ressurskommentar");
        ResourceElement resource = client.getResource("mytestenv", "fullScopeResource", ResourceTypeDO.BaseUrl, DomainDO.Devillo, "myApp");
        assertNotNull(resource);
        assertEquals("mytestenv", resource.getEnvironmentName());
        assertEquals("u", resource.getEnvironmentClass());
        assertEquals(DomainDO.Devillo, resource.getDomain());
        assertEquals("myApp", resource.getApplication());
    }

    @Test
    public void registerResourceMultipart() throws IOException {

        MultipartFormDataOutput data = new MultipartFormDataOutput();
        data.addFormData("alias", "mintjeneste", MediaType.TEXT_PLAIN_TYPE);
        data.addFormData("scope.environmentclass", "u", MediaType.TEXT_PLAIN_TYPE);
        data.addFormData("scope.environmentname", "myTestEnv", MediaType.TEXT_PLAIN_TYPE);
        data.addFormData("scope.domain", "devillo.no", MediaType.TEXT_PLAIN_TYPE);
        data.addFormData("scope.application", "myApp", MediaType.TEXT_PLAIN_TYPE);
        data.addFormData("type", ResourceTypeDO.Certificate, MediaType.TEXT_PLAIN_TYPE);

        data.addFormData("keystorealias", "app-key", MediaType.TEXT_PLAIN_TYPE);
        data.addFormData("keystorepassword", "keystoresecret", MediaType.TEXT_PLAIN_TYPE);
        data.addFormData("keystore.filename", "keystore.jks", MediaType.TEXT_PLAIN_TYPE);
        data.addFormData("keystore.file", "dilldalldull".getBytes(), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        // check results
        ResourceElement resource = client.executeMultipart("PUT", "resources", data, "comment med multipart", ResourceElement.class);
        assertNotNull(resource);
        assertEquals("mintjeneste", resource.getAlias());
        assertEquals("mytestenv", resource.getEnvironmentName());
        assertEquals("u", resource.getEnvironmentClass());
        assertEquals(DomainDO.Devillo, resource.getDomain());
        assertEquals("myApp", resource.getApplication());
        assertEquals("app-key", resource.getPropertyString("keystorealias"));
        URI keystoreUrl = resource.getPropertyUri("keystore");
        assertNotNull(keystoreUrl);
        InputStream keystoreFile = client.getFile(keystoreUrl);
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(keystoreFile, stringWriter);
        assertEquals("dilldalldull", stringWriter.getBuffer().toString());
        URI passwordUri = resource.getPropertyUri("keystorepassword");
        assertEquals("keystoresecret", client.getSecret(passwordUri));

        Resource storedResource = repository.getById(Resource.class, resource.getId());
        assertLastRevisionMessage(storedResource, "comment med multipart");
    }

    private ResourceElement createResourceElement(String alias) {
        ResourceElement resource = new ResourceElement(ResourceTypeDO.BaseUrl, alias);
        resource.setEnvironmentClass("u");
        resource.addProperty(new PropertyElement("url", "http://der/her"));
        return resource;
    }

    @Test
    public void testGetApplicationInfo_notFound() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            client.getApplicationInfo("unknown");
        });
    }

    @Test
    public void testGetClusters_notFound() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            client.getApplicationInstance("myTestEnv", "unknown");
        });
    }

}
