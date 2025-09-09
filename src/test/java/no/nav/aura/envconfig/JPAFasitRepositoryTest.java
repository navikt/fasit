package no.nav.aura.envconfig;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;
import no.nav.aura.envconfig.util.TestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static no.nav.aura.envconfig.model.infrastructure.Domain.Devillo;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.u;
import static no.nav.aura.envconfig.model.resource.ResourceType.DataSource;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {SpringUnitTestConfig.class})
@Transactional
@Rollback
public class JPAFasitRepositoryTest {

    @Autowired
    private FasitRepository repository;
    private Application application;
    private ApplicationGroup singleApplicationGroup;
    private ApplicationGroup multiApplicationGroup;
    private Environment t1;
    private Cluster clusterTestLocal;
    private Node nodeTestLocal;

    private final String appGroupWithOneApplication = "appGroupWithOneApplication";
    private final static String appGroupWithMultipleApplications = "appGroupWithMultipleApplications";

    @BeforeEach
    public void setup() throws Exception {
        String appName = "tsys";

        application = repository.store(new Application(appName));
        singleApplicationGroup = repository.store(new ApplicationGroup(appGroupWithOneApplication, application));

        multiApplicationGroup = repository.store(new ApplicationGroup(appGroupWithMultipleApplications));
        multiApplicationGroup.addApplication(createApplication("myFirstApp"));
        multiApplicationGroup.addApplication(createApplication("mySecondApp"));
        multiApplicationGroup.addApplication(createApplication("myThirdApp"));

        repository.store(multiApplicationGroup);

        t1 = new Environment("t1", EnvironmentClass.t);
        clusterTestLocal = new Cluster(appName, Domain.TestLocal);
        t1.addCluster(clusterTestLocal);
        clusterTestLocal.addApplication(application);
        nodeTestLocal = new Node("node1.test.local", "deployer", "", t1.getEnvClass(), PlatformType.JBOSS);
        t1.addNode(clusterTestLocal, nodeTestLocal);

        Cluster clusterOeraT = new Cluster(appName, Domain.OeraT);
        Node node_1 = new Node("a1234.oerat.no", "srvyo", "s45oo1xxs1Z", t1.getEnvClass(), PlatformType.JBOSS);
        clusterOeraT.addNode(node_1);
        clusterOeraT.addApplication(application);

        t1 = repository.store(t1);
        clusterTestLocal = TestHelper.assertAndGetSingle(t1.getClusters());
        nodeTestLocal = TestHelper.assertAndGetSingle(t1.getNodes());
        repository.store(clusterOeraT);
    }
    
    @AfterEach
    public void tearDown() {
        repository.delete(t1);
        repository.delete(singleApplicationGroup);
        repository.delete(application);
//    	clusterTestLocal.removeNode(nodeTestLocal);
//        repository.store(clusterTestLocal);
//
//    	repository.delete(nodeTestLocal);
//    	repository.delete(clusterTestLocal);
//    	repository.delete(t1);
//    	repository.delete(singleApplicationGroup);
//		repository.delete(application);
	}

    @Test
    public void findEnvironment() {
        repository.store(new Environment("u1", EnvironmentClass.t));
        assertEquals(2, repository.findEnvironmentsBy(EnvironmentClass.t).size(), "find by class");
        assertNotNull(repository.findEnvironmentBy("u1"), "get");
    }

    @Test
    public void updateEnvironment() {
        Environment environment = repository.findEnvironmentBy("t1");
        assertNotNull(environment);
        Cluster cluster = environment.addCluster(new Cluster("newCluster", Domain.TestLocal));
        environment.addNode(cluster, new Node("host", "username", "password", environment.getEnvClass(), PlatformType.JBOSS));
        repository.store(environment);
        Environment env2 = repository.findEnvironmentBy("t1");
        assertEquals(2, env2.getClusters().size());

    }

    @Test
    public void getEnvironmentNotFound() {
        assertNull(repository.findEnvironmentBy("unknown"));
    }

    @Test
    public void verifyStorage() throws Exception {
        Environment environment = repository.findEnvironmentBy("t1");
        assertNotNull(environment);
        assertEquals(1, environment.getClusters().size(), "clusters");
        assertEquals(1, environment.getApplications().size(), "apps");
        assertEquals(1, environment.getClusters().iterator().next().getNodes().size(), "nodes");
    }

    @Test
    public void verifyStorageResource() throws Exception {
        Application myapp = repository.store(new Application("myApp"));

        Scope scope = new Scope(EnvironmentClass.u);
        scope.domain(Domain.Devillo);
        scope.envName("u1");
        scope.application(myapp);

        Resource res = new Resource("myResource", ResourceType.BaseUrl, scope);
        res.putPropertyAndValidate("url", "http://value");
        Resource stored = repository.store(res);
        assertNotNull(stored.getScope().getApplication());
    }

    @Test
    public void addAndFindNode() throws Exception {
        Environment envZone = repository.findEnvironmentBy("t1");
        assertEquals(1, envZone.getClusters().iterator().next().getNodes().size(), "nodes");
        String clusterName = "tsys";
        Node node =new Node("a1236.test.local", "srvyo", "pass");
        envZone.findClusterByName(clusterName).addNode(node);
        envZone.addNode(node);
        repository.store(envZone);
        envZone = getEnvironment();
        assertEquals(2, envZone.getClusters().iterator().next().getNodes().size(), "nodes");
    }

    @Test
    public void addAndFindDatabaseSchema() throws Exception {
        Environment env = getEnvironment();
        String alias = "tsysDb";
        String username = "tsysDBuser";
        String url = "jdbc:oracle:thin:@dm02db04.adeo.no:1521:unav05";
        createDatabase(alias, env.getScope(), url, username, "secret");
        repository.store(application);
        List<Resource> resources = repository.findResourcesByExactAlias(env.getScope().application(application), ResourceType.DataSource, alias);
        assertEquals(1, resources.size(), "found resources");
        Resource dbSchemaResource = resources.get(0);
        assertNotNull(dbSchemaResource);
        assertEquals(alias, dbSchemaResource.getAlias(), "dbschema alias");
        assertEquals(url, dbSchemaResource.getProperties().get("url"), "dbschema url");
        Secret secret = dbSchemaResource.getSecrets().get("password");
        assertEquals(username, dbSchemaResource.getProperties().get("username"), "dbschema password");
        assertEquals("secret", secret.getClearTextString(), "dbschema password");
    }

    private Environment getEnvironment() {
        return repository.findEnvironmentBy("t1");
    }

    private Resource createDatabase(String alias, Scope scope, String url, String username, String password) {
        Resource resource = new Resource(alias, ResourceType.DataSource, scope);
        resource.putPropertyAndValidate("url", url);
        resource.putPropertyAndValidate("username", username);
        resource.putSecretAndValidate("password", password);
        return (Resource) repository.store(resource);
    }

    @Test
    public void addNodeToCluster() {
        Environment environment = getEnvironment();
        Cluster cluster = getCluster(environment);
        assertEquals(1, cluster.getNodes().size());
        Node node = new Node("heihost.test.local", "sa", "password");
        cluster.addNode(node);
        environment.addNode(node);
        repository.store(environment);
        assertEquals(2, getCluster(getEnvironment()).getNodes().size());
    }

    private Cluster getCluster(Environment environment) {
        assertNotNull(environment);
        Set<Cluster> clusters = environment.getClusters();
        assertEquals(1, clusters.size());
        return clusters.iterator().next();
    }

    @Test
    public void getResourcesForEnvironmentClass() {
        assertEquals(0, repository.findResourcesByExactAlias(new Scope(EnvironmentClass.t), null, null).size());
        repository.store(createDatabase("tull", new Scope(EnvironmentClass.t), "jdbc::tull", "sa", "pwd"));
        assertEquals(1, repository.findResourcesByExactAlias(new Scope(EnvironmentClass.t), null, null).size());
        assertEquals(0, repository.findResourcesByExactAlias(new Scope(EnvironmentClass.p), null, null).size());
        assertEquals(1, repository.findResourcesByExactAlias(new Scope(), null, null).size());
    }

    @Test
    public void getResourcesForEnvironmentWithCorrectUniquness() {
        Environment environment = getEnvironment();
        Environment environmentWithWrongEnvironmentClass = new Environment("otherEnv", EnvironmentClass.p);
        Environment environmentWithWrongName = new Environment("WrongName", EnvironmentClass.t);
        repository.store(environmentWithWrongEnvironmentClass);
        repository.store(environmentWithWrongName);
        assertEquals(0, repository.findResourcesByExactAlias(environment.getScope(), null, null).size());
        repository.store(createDatabase("tull", environment.getScope(), "jdbc::tull", "sa", ""));
        assertEquals(1, repository.findResourcesByExactAlias(environment.getScope(), null, null).size());
        assertEquals(0, repository.findResourcesByExactAlias(environmentWithWrongEnvironmentClass.getScope(), null, null).size());
    }

    @Test
    public void sameClusterNameCanExistInDifferentEnvironmentZones() {
        Environment environment = getEnvironment();
        environment.addCluster(new Cluster("TulleKl�ster", Domain.TestLocal));
        Environment environment2 = new Environment("NyttMilj�", EnvironmentClass.t);
        repository.store(environment);
        environment2.addCluster(new Cluster("TulleKl�ster", Domain.TestLocal));
        repository.store(environment2);
    }

    @Test
    public void sameApplicationNameCanExistInDifferentEnvironmentZones() {
        Environment environment = getEnvironment();
        Application app = repository.store(new Application("ST2000"));
        environment.getClusters().iterator().next().addApplication(app);
        repository.store(environment);
        Environment environmentZone2 = new Environment("NyttMilj�", EnvironmentClass.t);

        Cluster cluster = new Cluster("ST2000-cluster", Domain.TestLocal);
        environmentZone2.addCluster(cluster);

        cluster.addApplication(app);
        repository.store(environmentZone2);
    }

    @Test
    public void findEnvironmentByClass() {
        Environment environment = getEnvironment();
        repository.store(environment);

        List<Environment> envs = repository.findEnvironmentsBy(EnvironmentClass.t);
        assertEquals(1, envs.size());
        Environment tenv = envs.iterator().next();
        assertEquals("t1", tenv.getName());
        assertEquals(EnvironmentClass.t, tenv.getEnvClass());
    }

    @Test
    public void findEnvironmentByCluster() {
        assertThat(repository.getEnvironmentBy(clusterTestLocal), is(getEnvironment()));
    }

    @Test
    public void createClusterWithNode() {
        Environment environment = repository.store(new Environment("NyttEnv", EnvironmentClass.u));
        Node node = new Node("der", "han", "pass", environment.getEnvClass(), PlatformType.JBOSS);
        environment.addNode(node);
        Cluster cluster = new Cluster("C", Domain.Devillo);
        cluster.addNode(node);
        cluster.setName("C");
        environment.addCluster(cluster);
        repository.store(environment);
    }

    @Test
    public void findAndDeleteNode() {
        Node node = repository.findNodeBy("node1.test.local");
        assertNotNull(node, "found node");
        assertNull(repository.findNodeBy("shouldnotbefound"), "should not be found");
        repository.delete(node);
        Node node2 = repository.findNodeBy("node1.test.local");
        assertNull(node2, "deleted");
    }

    @Test
    public void getApplicationInstancesByApplication() {
        List<ApplicationInstance> applicationInstances = createAndGetApplicationInstance(t1);
        assertEquals(3, applicationInstances.size());
    }

    private List<ApplicationInstance> createAndGetApplicationInstance(Environment environment) {
        Cluster cluster = new Cluster("tull", Domain.Devillo);
        cluster.addApplication(application);
        environment.addCluster(cluster);
        environment.addNode(cluster, new Node(UUID.randomUUID().toString(), "root", "pass", environment.getEnvClass(), PlatformType.JBOSS));
        repository.store(environment);
        return repository.findApplicationInstancesBy(application);
    }

    @Test
    public void getEnvironmentPartsForClass() {
        repository.store(new Environment("tull", EnvironmentClass.u));
        repository.store(new Environment("tall", EnvironmentClass.u));
        assertEquals(2, repository.findEnvironmentsBy(EnvironmentClass.u).size());
        assertEquals(1, repository.findEnvironmentsBy(EnvironmentClass.t).size());
    }

    @Test
    public void checkGetEnvironment() {
        assertNull(repository.findEnvironmentBy("ikkeEksisterende"));
        assertNotNull(repository.findEnvironmentBy(t1.getName()));
    }

    @Test
    public void getEnvironments() {
        repository.store(new Environment("bongobongo", EnvironmentClass.u));
        assertEquals(2, repository.getEnvironments().size());
        List<Environment> uEnvs = repository.findEnvironmentsBy(EnvironmentClass.u);
        assertEquals(1, uEnvs.size());
        assertEquals("bongobongo", uEnvs.iterator().next().getName());
        List<Environment> tEnvs = repository.findEnvironmentsBy(EnvironmentClass.t);
        assertEquals(1, tEnvs.size());
        assertEquals("t1", tEnvs.iterator().next().getName());
    }

    @Test
    public void getApplications() {
        assertEquals(4, repository.getApplications().size());
    }

    @Test
    public void findApplicationByName() {
        assertNull(repository.findApplicationByName("Tull"));
        assertNotNull(repository.findApplicationByName("tsys"));
        Application caseInsensitive = repository.store(new Application("cAsingTeST"));
        assertThat("Find application should ignore casing", repository.findApplicationByName("casingtest"), is(caseInsensitive));
    }

    @Test
    public void getConnectionDescription() {
        assertTrue(repository.getConnectionDescription().contains("jdbc"));
    }

    @Test
    public void findDuplicateResources() {
        Resource original = repository.store(createResource("test1", "http://host:80/hark", "pa"));
        repository.store(createResource("test1", "http://host:80/ptui", "pa"));
        repository.store(createResource("test1", "http://host:80/hark", "pb"));
        Resource newResource = createResource("annen", "http://host:80/hark", "pa");
        assertEquals(new ArrayList<>(Arrays.asList(original)), repository.findDuplicateProperties(newResource));
        newResource = repository.store(newResource);
        assertEquals(new ArrayList<>(Arrays.asList(original)), repository.findDuplicateProperties(newResource));
    }

    private Resource createResource(String alias, String url, String userName) {
        Resource resource = new Resource(alias, ResourceType.DataSource, new Scope(EnvironmentClass.u));
        resource.putPropertyAndValidate("url", url);
        resource.putPropertyAndValidate("username", userName);
        resource.putPropertyAndValidate("oemEndpoint", "test");
        resource.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        return resource;
    }

    private Environment createEnvWithClusterInT() {
        Environment env = new Environment("env", EnvironmentClass.t);
        env.addCluster(new Cluster("cluster", Domain.TestLocal));
        env = repository.store(env);
        return env;
    }

    @Test
    public void findOverlappingResourceScope_environmentScope() {
        Scope scope = new Scope(EnvironmentClass.t);
        Resource resource = repository.store(new Resource("a", ResourceType.DataSource, scope));
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("b", ResourceType.DataSource, scope)).size());
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.BaseUrl, scope)).size());
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(EnvironmentClass.u))).size());
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(t1).domain(null))).size());
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(t1))).size());
        assertEqualResource(resource, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, scope)));
    }

    private void assertEqualResource(Resource resource, List<Resource> resources) {
        assertEquals(1, resources.size());
        assertEquals(resource.getID(), resources.iterator().next().getID());
    }

    @Test
    public void findOverlappingResourceScope_complete() {
        Scope scope = new Scope(t1).application(application);
        Resource resource = repository.store(new Resource("a", ResourceType.DataSource, scope));
        Application appTull = repository.store(new Application("tull"));
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(scope).application(null))).size());
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(scope).application(appTull))).size());
        assertEquals(1, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(scope).domain(null))).size());
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(scope).domain(Domain.TestLocal))).size());
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(scope).envName(null))).size());
        assertEquals(0, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, new Scope(scope).envName("doh"))).size());
        assertEqualResource(resource, repository.findOverlappingResourceScope(new Resource("a", ResourceType.DataSource, scope)));
    }

    @Test
    public void getApplicationInstanceByExposedResourceId_notAnExposedResource() {
        Resource resource = repository.store(new Resource("tull", ResourceType.DataSource, new Scope(EnvironmentClass.u)));
        repository.findApplicationInstanceByExposedResourceId(resource.getID());
    }

    @Test
    public void getApplicationInstanceByExposedResourceId_anExposedResource() {
        // t1.addCluster(new Cluster("tull"));
        Cluster cluster = TestHelper.assertAndGetSingle(t1.getClusters());
        // cluster.addApplication(application);
        ApplicationInstance applicationInstance = TestHelper.assertAndGetSingle(cluster.getApplicationInstances());
        Resource resource = new Resource("tull", ResourceType.DataSource, new Scope(EnvironmentClass.u));
        ExposedServiceReference expServiceRef = new ExposedServiceReference(resource, null);
        applicationInstance.getExposedServices().add(expServiceRef);
        t1 = repository.store(t1);
        cluster = TestHelper.assertAndGetSingle(t1.getClusters());
        applicationInstance = TestHelper.assertAndGetSingle(cluster.getApplicationInstances());
        expServiceRef = TestHelper.assertAndGetSingle(applicationInstance.getExposedServices());
        ApplicationInstance exposingApplicationInstance = repository.findApplicationInstanceByExposedResourceId(expServiceRef.getResource().getID());
        assertNotNull(exposingApplicationInstance);
        assertEquals(applicationInstance.getID(), exposingApplicationInstance.getID());
    }

    @Test
    public void getCount() {
        assertEquals(4, repository.count(Application.class));
        repository.store(new Application("Dall"));
        assertEquals(5, repository.count(Application.class));
    }

    @Test
    public void findFutureResourceReferencesBy() {
        ApplicationInstance applicationInstance = clusterTestLocal.addApplication(new Application("vas"));
        applicationInstance.getResourceReferences().add(ResourceReference.future("tull", ResourceType.WebserviceEndpoint));
        applicationInstance = repository.store(applicationInstance);
        assertEquals(0, repository.findFutureResourceReferencesBy("tull", ResourceType.Certificate).size());
        assertEquals(1, repository.findFutureResourceReferencesBy("tull", ResourceType.WebserviceEndpoint).size());

        Set<ResourceReference> resourceReferences = applicationInstance.getResourceReferences();
        assertEquals(1, applicationInstance.getResourceReferences().size());
        ResourceReference unfuture = resourceReferences.iterator().next();
        unfuture.setResource(new Resource("tull", ResourceType.WebserviceEndpoint, new Scope(EnvironmentClass.u)));
        repository.store(unfuture);
        assertEquals(0, repository.findFutureResourceReferencesBy("tull", ResourceType.WebserviceEndpoint).size());
    }

    @Test
    public void findFutureResourceReferencesBy_withoutApplicationInstanceShouldNotBePossibleButItWasAndNowWeWillHaveToFilterThose() {
        repository.store(ResourceReference.future("tull", ResourceType.WebserviceEndpoint));
        assertEquals(0, repository.findFutureResourceReferencesBy("tull", ResourceType.WebserviceEndpoint).size());
    }

    @Test
    public void findApplicationInstanceBy() {
        Cluster cluster = new Cluster("kl�ster", Domain.Devillo);
        ApplicationInstance applicationInstance = cluster.addApplication(new Application("Hei"));
        applicationInstance.getResourceReferences().add(ResourceReference.future("ball", ResourceType.WebserviceEndpoint));
        cluster = repository.store(cluster);
        applicationInstance = TestHelper.assertAndGetSingle(cluster.getApplicationInstances());
        ResourceReference resourceReference = TestHelper.assertAndGetSingle(applicationInstance.getResourceReferences());
        assertEquals(applicationInstance.getID(), repository.getApplicationInstanceBy(resourceReference).getID());
    }

    @Test
    public void getApplicationGroups() {
        Collection<ApplicationGroup> applicationGroups = repository.getApplicationGroups();
        assertEquals(2, applicationGroups.size());
        ApplicationGroup applicationGroup = applicationGroups.iterator().next();
        assertTrue(applicationGroups.containsAll(new ArrayList<>(Arrays.asList(singleApplicationGroup, multiApplicationGroup))));
    }

    @Test
    public void findApplicationGroupByName() {
        assertNull(repository.findApplicationGroupByName("myNonExistingApplicationGroup"));
        ApplicationGroup applicationGroup = repository.findApplicationGroupByName(appGroupWithOneApplication);
        assertNotNull(applicationGroup);
        assertEquals("tsys", applicationGroup.getApplications().iterator().next().getName());
    }

    @Test
    public void findApplicationGroupForApplication() {
        Cluster cluster = new Cluster("myCluster", Domain.TestLocal);
        repository.store(cluster.addApplication(application));
        assertThat(repository.findApplicationGroup(cluster.getApplications()).isPresent(), is(true));
        assertThat(repository.findApplicationGroup(application), is(singleApplicationGroup));
    }

    @Test
    public void whenApplicationIsNotMappedToAnyGroup_noApplicationGroupShouldBeFoundForTheApplicationInstance() {
        Application applicationNotInGroup = repository.store(new Application("appNotInAppGroup"));
        Cluster cluster = new Cluster("myCluster", Domain.TestLocal);
        ApplicationInstance applicationInstance = repository.store(cluster.addApplication(applicationNotInGroup));
        assertThat(repository.findApplicationGroup(cluster.getApplications()).isPresent(), is(false));
        assertNull(repository.findApplicationGroup(applicationInstance.getApplication()));
    }

    @Test
    public void findAllApplicationsNotInAnyApplicationGroup() {
        repository.store(new Application("NotInAnyApplicationGroup", null, null));
        assertEquals(1, repository.getApplicationsNotInApplicationGroup().size());
    }

    @Test
    public void removeApplicationFromGroup() {
        singleApplicationGroup.removeApplication(application);
        repository.store(singleApplicationGroup);
        assertEquals(0, repository.findApplicationGroupByName(appGroupWithOneApplication).getApplications().size());
    }

    @Test
    public void removeApplicationGroupById() {
        multiApplicationGroup.removeApplicationByApplicationId(repository.findApplicationByName("mySecondApp").getID());
        repository.store(multiApplicationGroup);
        assertEquals(2, repository.findApplicationGroupByName(appGroupWithMultipleApplications).getApplications().size());
    }

    @Test
    public void whenRemovingUnknownApplicationId_exceptionShouldBeThrown() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            long nonExistingId = 1234;
            multiApplicationGroup.removeApplicationByApplicationId(nonExistingId);
        });

    }

    @Test
    public void whenOneApplicationInApplicationGroup_portOffsetShouldBeZeroForTheApplication() {
        Collection<ApplicationGroup> applicationGroups = repository.getApplicationGroups();
        assertEquals(0, repository.findApplicationByName("tsys").getPortOffset());
    }

    @Test
    public void whenMultipleApplicationsInGroup_appsInGroupShouldHaveDifferentPortOffset() {
        assertPortOffsetIsCorrectForApplications();
    }

    @Test
    public void whenApplicationIsRemovedFromApplicationGroup_portOffsetShouldRemainUnchanged() {
        multiApplicationGroup.removeApplication(repository.findApplicationByName("mySecondApp"));
        repository.store(multiApplicationGroup);
        assertEquals(2, repository.findApplicationGroupByName(appGroupWithMultipleApplications).getApplications().size());
        assertPortOffsetIsCorrectForApplications();
    }

    @Test
    public void whenAppIsAddedAfterAnOtherIsRemoved_theNewlyAddedAppShouldGetLowestPortOffsetAvailable() {
        multiApplicationGroup.removeApplication(repository.findApplicationByName("mySecondApp"));
        multiApplicationGroup.addApplication(createApplication("yetAnotherApp"));
        repository.store(multiApplicationGroup);
        assertEquals(1, repository.findApplicationByName("yetAnotherApp").getPortOffset());
    }

    @Test
    public void findEveryClusterThatAnApplicationGroupIsMappedTo() {
        Cluster firstCluster = createClusterWithApplicationGroup("myFirstCluster", multiApplicationGroup, Domain.Devillo);
        Cluster secondCluster = createClusterWithApplicationGroup("mySecondCluster", multiApplicationGroup, Domain.TestLocal);
        Cluster clusterWithoutApplicationGroup = createClusterWithApplication("myClusterWithoutApplicationGroup", Domain.TestLocal);

        Set<Cluster> clustersInApplicationGroup = repository.findClustersBy(multiApplicationGroup);
        assertThat(clustersInApplicationGroup, containsInAnyOrder(firstCluster, secondCluster));
        assertThat(clustersInApplicationGroup, not(contains(clusterWithoutApplicationGroup)));
    }

    @Test
    public void testCaseInsensitiveResourceFilter() {
        Scope scope = new Scope(u).domain(Devillo).envName("u1");
        Resource storedResource = repository.store(new Resource("myResourceAlias", DataSource, scope));

        List<Resource> resources = repository.findResourcesByLikeAlias(scope, null, storedResource.getAlias().toUpperCase(), 0, 10, "alias", true);
        Resource fetchedResource = resources.get(0);

        assert (resources.size() == 1);
        assertEquals(storedResource.getAlias(), fetchedResource.getAlias());
    }

    private Cluster createClusterWithApplicationGroup(String name, ApplicationGroup applicationGroup, Domain domain) {
        Cluster cluster = repository.store(new Cluster(name, domain));
        cluster.addApplicationGroup(applicationGroup);
        return repository.store(cluster);
    }

    private Cluster createClusterWithApplication(String name, Domain domain) {
        Cluster cluster = repository.store(new Cluster(name, domain));
        cluster.addApplication(repository.store(new Application("myAppNotInAnyApplicationGroup")));
        return repository.store(cluster);
    }

    private void assertPortOffsetIsCorrectForApplications() {
        assertEquals(0, repository.findApplicationByName("myFirstApp").getPortOffset());
        assertEquals(1, repository.findApplicationByName("mySecondApp").getPortOffset());
        assertEquals(2, repository.findApplicationByName("myThirdApp").getPortOffset());
    }

    private Application createApplication(String applicationName) {
        Application entity = new Application(applicationName);
        return repository.store(entity);
    }

    @Test
    public void testScopeFilterEnvironmentSorting() {
        repository.store(new Environment("t10", EnvironmentClass.t));
        repository.store(new Environment("t20", EnvironmentClass.t));
        repository.store(new Environment("t2", EnvironmentClass.t));

        List<Environment> storedEnvironments = repository.getEnvironments();

        List<String> expectedEnvName = new ArrayList<>();
        expectedEnvName.add("t1");
        expectedEnvName.add("t10");
        expectedEnvName.add("t2");
        expectedEnvName.add("t20");

        List<String> storedEnvironmentNames = environmentsToName(storedEnvironments);

        assertEquals(expectedEnvName, storedEnvironmentNames);
    }

    private List<String> environmentsToName(List<Environment> environments) {
        List<String> environmentNames = new ArrayList<String>();

        for (Environment environment : environments) {
            environmentNames.add(environment.getName());
        }

        return environmentNames;
    }
    
    private Cluster findClusterContainingNode(Node node) {
        Collection<Cluster> allClusters = repository.getAll(Cluster.class);
        for (Cluster cluster : allClusters) {
            if (cluster.getNodes().contains(node)) {
                return cluster;
            }
        }
        return null;
    }
}
