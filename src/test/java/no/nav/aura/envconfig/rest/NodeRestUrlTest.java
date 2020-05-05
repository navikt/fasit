package no.nav.aura.envconfig.rest;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.ws.rs.core.Response.Status;

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import io.restassured.http.ContentType;

public class NodeRestUrlTest extends RestTest {

    private static Environment testEnv;
    private static Node node;

    @BeforeAll
    public static void setUp() throws Exception {
        Environment utvEnv = repository.store(new Environment("myUtvEnv", EnvironmentClass.u));

        Cluster appCluster = utvEnv.addCluster(new Cluster("cluster", Domain.Devillo));
        Application app = repository.store(new Application("myApp"));
        appCluster.addApplication(app);
        Node newNode = new Node("myNewHost.devillo.no", "username", "password");
        utvEnv.addNode(appCluster, newNode);
        appCluster.addNode(newNode);

        Application appInGrp1 = repository.store(new Application("myFirstAppInGroup"));
        Application appInGrp2 = repository.store(new Application("mySecondAppInGroup"));
        repository.store(new ApplicationGroup("myAppGrp", Lists.newArrayList(
                appInGrp1,
                appInGrp2)));
        Cluster groupCluster = utvEnv.addCluster(new Cluster("groupCluster", Domain.Devillo));
        groupCluster.addApplication(appInGrp1);
        groupCluster.addApplication(appInGrp2);
        Node groupNode = new Node("grouphost.devillo.no", "username", "password");
        utvEnv.addNode(groupCluster, groupNode);
        repository.store(utvEnv);
        testEnv = repository.store(new Environment("myTestEnv", EnvironmentClass.t));
        node = repository.findNodeBy(newNode.getHostname());

    }

    @Test
    public void nodeGetServices_shouldBeCaseInsensitive() throws Exception {
        String envName = "env";
        Environment env = new Environment(envName, EnvironmentClass.u);
        String hostname = "hostname.devillo.no";
        Node node = new Node(hostname, "", "");
        env.addNode(node);
        repository.store(env);

        expect().statusCode(OK.getStatusCode()).body(containsString(hostname)).when().get("/conf/nodes?envName=" + envName);
        expect().statusCode(OK.getStatusCode()).body(containsString(hostname)).when().get("/conf/nodes?envName=" + envName.toUpperCase());
        expect().statusCode(OK.getStatusCode()).body(containsString(hostname)).when().get("/conf/nodes/" + hostname);
        expect().statusCode(OK.getStatusCode()).body(containsString(hostname)).when().get("/conf/nodes/" + hostname.toUpperCase());
    }

    @Test
    public void getNodeWithSingleApp() {
        expect().
                statusCode(OK.getStatusCode())
                .body(containsString("<hostname>myNewHost.devillo.no"))
                .body(containsString("<username>username"))
                .body(containsString("<applicationMappingName>myApp"))
                .body(containsString("<applicationName>myApp"))
                .body(containsString("<environmentName>myutvenv"))
                .body(containsString("<environmentClass>u"))
                .when().get("/conf/nodes/myNewHost.devillo.no");
    }

    @Test
    public void getNodeWithApplicationGroup() {
        expect().
                statusCode(OK.getStatusCode())
                .body(containsString("<hostname>grouphost.devillo.no"))
                .body(containsString("<username>username"))
                .body(containsString("<applicationMappingName>myAppGrp"))
                .body(containsString("<applicationName>myFirstAppInGroup"))
                .body(containsString("<applicationName>mySecondAppInGroup"))
                .body(containsString("<environmentName>myutvenv"))
                .body(containsString("<environmentClass>u"))
                .when().get("/conf/nodes/grouphost.devillo.no");
    }

    @Test
    public void getNodeNotFound() {
        expect().statusCode(NOT_FOUND.getStatusCode()).when().get("/conf/nodes/unknown");
    }

    @Test
    public void findAllNodes() {
        Environment u13 = repository.store(new Environment("u13", EnvironmentClass.u));
        u13.addNode(new Node("node13_1.devillo.no", "username", "password"));
        u13.addNode(new Node("node13_2.devillo.no", "username", "password"));
        repository.store(u13);

        Environment t14 = repository.store(new Environment("t14", EnvironmentClass.t));
        t14.addNode(new Node("node14_1.test.local", "username", "password"));
        repository.store(t14);

        expect().
                statusCode(OK.getStatusCode())
                .body(containsString("<hostname>node13_1.devillo.no"))
                .body(containsString("<hostname>node13_2.devillo.no"))
                .body(containsString("<hostname>node14_1.test.local"))
                .when().get("/conf/nodes");
    }

    @Test
    public void findNodesByEnvironmentName() {
        Environment u2 = new Environment("u2", EnvironmentClass.u);
        Node node = new Node("node2.devillo.no", "username", "password");
        u2.addNode(node);
        repository.store(u2);

        expect().
                statusCode(OK.getStatusCode())
                .body(not(containsString("<hostname>myNewHost.devillo.no")))
                .body(containsString("<hostname>node2.devillo.no"))
                .when().get("/conf/nodes?envName=u2");
    }

    @Test
    @Disabled
    public void deleteNode() {
        given()
                .auth().basic("prodadmin", "prodadmin")
                .queryParam("entityStoreComment", "deleteComment")
                .expect().statusCode(Status.NO_CONTENT.getStatusCode()).when().delete("/conf/nodes/myNewHost.devillo.no");

        FasitRevision<Node> headrevision = getHeadrevision(node);
        assertEquals("deleteComment", headrevision.getMessage());
    }

    @Test
    public void deleteDmgr() {
        Resource storedDmgr = createDmgr("mydmgr.devillo.no");

        given().auth().basic("prodadmin", "prodadmin").expect().statusCode(Status.OK.getStatusCode()).when().get("/conf/resources/" + storedDmgr.getID());
        given().auth().basic("prodadmin", "prodadmin").expect().statusCode(Status.NO_CONTENT.getStatusCode()).when().delete("/conf/nodes/mydmgr.devillo.no");
        given().auth().basic("prodadmin", "prodadmin").expect().statusCode(Status.NOT_FOUND.getStatusCode()).when().get("/conf/resources/" + storedDmgr.getID());
    }

    @Test
    public void deletingNonExistentDmgr_shouldDoNothing() throws Exception {
        Resource storedDmgr = createDmgr("dmgr.devillo.no");

        given().auth().basic("prodadmin", "prodadmin").delete("/conf/nodes/feil.devillo.no");
        given().auth().basic("prodadmin", "prodadmin").expect().statusCode(Status.OK.getStatusCode()).when().get("/conf/resources/" + storedDmgr.getID());
    }

    @Test
    public void deleteNodeNotAuthorized() {
        expect().statusCode(Status.UNAUTHORIZED.getStatusCode()).when().delete("/conf/nodes/myNewHost.devillo.no");
    }

    @Test
    public void deleteNodeNotFound() {
        given().auth().basic("prodadmin", "prodadmin").expect().expect().statusCode(Status.NOT_FOUND.getStatusCode()).when().delete("/conf/nodes/unknown");
    }

    @Test
    public void updateNodeNotAuthenticated() {
        String content = "<node><status>STOPPED</status></node>";
        given().body(content)
                .contentType(ContentType.XML)
                .expect()
                .statusCode(Status.UNAUTHORIZED.getStatusCode())
                .when().post("/conf/nodes/myNewHost.devillo.no");
    }

    @Test
    public void updateNodeNotFound() {
        String content = "<node><status>STOPPED</status></node>";
        given().body(content)
                .auth().basic("prodadmin", "prodadmin")
                .contentType(ContentType.XML)
                .expect()
                .statusCode(Status.NOT_FOUND.getStatusCode())
                .when().post("/conf/nodes/unknown.devillo.no");
    }

    @Test
    public void updateNodeUnknownStatus() {
        String content = "<node><status>JUSTKIDDING</status></node>";
        given().body(content)
                .auth().basic("prodadmin", "prodadmin")
                .contentType(ContentType.XML)
                .expect()
                .statusCode(Status.BAD_REQUEST.getStatusCode())
                .when().post("/conf/nodes/myNewHost.devillo.no");
    }

    @Test
    public void stopNode() {
        String content = "<node><status>STOPPED</status></node>";
        given().auth()
                .basic("prodadmin", "prodadmin")
                .body(content)
                .contentType(ContentType.XML)
                .expect()
                .statusCode(Status.CREATED.getStatusCode())
                .when().post("/conf/nodes/myNewHost.devillo.no");

        expect().
                statusCode(OK.getStatusCode())
                .body(containsString("<hostname>myNewHost.devillo.no"))
                .body(containsString("<status>STOPPED"))
                .when().get("/conf/nodes/myNewHost.devillo.no");
    }

    /*@Test
    @Ignore
    public void startNode() {
        String content = "<node><status>STARTED</status></node>";
        given().auth()
                .basic("prodadmin", "prodadmin")
                .body(content)
                .contentType(ContentType.XML)
                .queryParam("entityStoreComment", "started node")
                .expect()
                .statusCode(Status.CREATED.getStatusCode())
                .when().post("/conf/nodes/myNewHost.devillo.no");

        expect().
                statusCode(OK.getStatusCode())
                .body(containsString("<hostname>myNewHost.devillo.no"))
                .body(not(containsString("<status>")))
                .when().get("/conf/nodes/myNewHost.devillo.no");

        FasitRevision<Node> headrevision = getHeadrevision(node);
        assertEquals("started node", headrevision.getMessage());
        assertEquals("prodadmin", headrevision.getAuthor());
        assertNull(headrevision.getOnbehalfOf());
    }*/

    @Test
    public void registerNode_mustHaveDomainEnvironmentAndApplication() {
        registerNode("<node><hostname>nodewithoutdomain</hostname></node>", 400);
    }

    @Test
    public void registerNode_registersNodeWithNonExistingEnvironment_shouldFail() {
        registerNode(createXml("notExistingEnv.devillo.no", "myEnv", "devillo.no", "myApp"), 400);
    }

    @Test
    public void registerNode_registersNodeWithExistingEnvironment() {
        registerNode(createXml("existingEnv.devillo.no", "myUtvEnv", "devillo.no", "myApp"), 200);
    }

    @Test
    public void registerNode_registersMoreNodesInTest() {
        registerNode(createXml("newHost.test.local", "myTestEnv", "test.local", "myApp"), 200);
        registerNode(createXml("newerHost.test.local", "myTestEnv", "test.local", "myApp"), 200);
    }

    @Test
    public void registerNode_TwoDomains() {
        repository.store(new Environment("t1", EnvironmentClass.t));

        registerNode(createXml("nodeInOera.oera-t.local", "t1", "oera-t.local", "myApp"), 200);
        registerNode(createXml("nodeInTestLocal.test.local", "t1", "test.local", "myApp"), 200);
    }

    @Test
    public void registerNode_registersNodeWithMismatchingDomain() {
        registerNode(createXml("mismatchDomain.test.local", "myTestEnv", "oera-t.local", "myApp"), 400);
    }

    @Test
    public void registerNode_registersNodeWithUnknownDomain() {
        registerNode(createXml("unknownDomain.devillo.no", "myUtvEnv", "devilloikke.no", "myApp"), 400);
    }

    @Test
    public void registerNode_failsWithNonExistingApp() {
        registerNode(createXml("wihtNunKnownApp.devillo.no", "myUtvEnv", "devillo.no", "nonExistingApp"), 400);
    }

    @Test
    public void registerNodeForApplicationGroup() {
        registerNode(createXml("multiApp.devillo.no", "myUtvEnv", "devillo.no", "myAppGrp"), 200);
    }

    @Test
    public void registerDuplicateNode_shouldFail() {
        registerNode(createXml("duplicate.devillo.no", "myUtvEnv", "devillo.no", "myApp"), 200);
        registerNode(createXml("duplicate.devillo.no", "myUtvEnv", "devillo.no", "myApp"), 400);
    }

    @Test
    public void registerMoreNodesForMultipleApplicationsInApplicationGroup() {
        registerNode(createXml("newHost.devillo.no", "myUtvEnv", "devillo.no", "myAppGrp"), 200);
        registerNode(createXml("newerHost.devillo.no", "myUtvEnv", "devillo.no", "myAppGrp"), 200);
    }

    @Test
    public void registerNodeForAppgroupWithWrongDomain() {
        registerNode(createXml("fssHost.test.local", testEnv.getName(), "test.local", "myAppGrp"), 200);
        registerNode(createXml("sbsHost.oera-t.local", testEnv.getName(), "oera-t.local", "myAppGrp"), 400);
    }

    @Test
    public void registerNodeWithExtraInfo() {
        registerNode(createXmlWithAdditionalInfo(), 200);
    }

    private void registerNode(String content, int expectedStatusCode) {
        given().auth()
                .basic("prodadmin", "prodadmin")
                .body(content)
                .contentType(ContentType.XML)
                .expect()
                .statusCode(expectedStatusCode)
                .when().put("/conf/nodes");

    }

    private String createXml(String hostname, String env, String domain, String applicationMapping) {
        String applicationNames = "<applicationMappingName>" + applicationMapping + "</applicationMappingName>";
        return "<node><hostname>" + hostname + "</hostname><environmentName>" + env
                + "</environmentName>" + applicationNames + " <domain>" + domain + "</domain><platformType>JBOSS</platformType></node>";
    }

    private String createXmlWithAdditionalInfo() {
        return "<node><hostname>hostWithAdditionalInfo.devillo.no</hostname><environmentName>myUtvEnv</environmentName>" +
                "<applicationMappingName>myApp</applicationMappingName><domain>devillo.no</domain><platformType>JBOSS</platformType>" +
                "<dataCenter>dataCenter1</dataCenter><memoryMb>2048</memoryMb><cpuCount>2</cpuCount></node>";
    }

    private static Resource createDmgr(String hostname) {
        Resource dmgr = new Resource("wasdmgr", ResourceType.DeploymentManager, new Scope(EnvironmentClass.u).domain(Domain.Devillo));
        dmgr.putPropertyAndValidate("hostname", hostname);
        dmgr.putPropertyAndValidate("username", "user");
        dmgr.putSecretAndValidate("password", "pass");
        return repository.store(dmgr);
    }

}
