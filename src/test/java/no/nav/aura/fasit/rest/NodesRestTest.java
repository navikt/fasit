package no.nav.aura.fasit.rest;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.rest.model.Link;
import no.nav.aura.fasit.rest.model.NodePayload;
import no.nav.aura.fasit.rest.model.SecretPayload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestInstance(Lifecycle.PER_CLASS)
public class NodesRestTest extends RestTest {

    private static final Logger log = LoggerFactory.getLogger(NodesRestTest.class);
    @Inject
    private ApplicationRepository applicationRepository;

	@Inject
	private EnvironmentRepository environmentRepo;

    @BeforeAll
    public void setUp() throws Exception {

        Environment u1 = new Environment("u1", EnvironmentClass.u);

        Cluster cluster = u1.addCluster(new Cluster("junit", Domain.Devillo));
        u1.addNode(cluster, new Node("test.devillo.no", "user", "secret"));
        u1.addNode(cluster, new Node("deleteme.devillo.no", "user", "secret"));
        u1.addNode(cluster, new Node("changeme.devillo.no", "junit", "secret"));
        u1 = environmentRepo.save(u1);

        Environment ipEnv = new Environment("ipEnv", EnvironmentClass.t);
        Node nodeWithIp = new Node("1.2.3.4", "somethin", "", ipEnv.getEnvClass(), PlatformType.JBOSS);
        nodeWithIp.setLifeCycleStatus(LifeCycleStatus.ALERTED);
        ipEnv.addNode(nodeWithIp);
        environmentRepo.save(ipEnv);

        Application app1 = applicationRepository.save(new Application("app1"));

        Environment u2 = new Environment("u2", EnvironmentClass.u);
        Cluster cluster2 = u2.addCluster(new Cluster("cluster2", Domain.Devillo));
        u2.addNode(cluster2, new Node("anotherNode.devillo.no", "user", "secret"));
        u2 = environmentRepo.save(u2);
        cluster2.addApplication(app1);
        environmentRepo.save(u2);
    }
    
    @AfterAll
    public void tearDown() {
		cleanupEnvironments();
		cleanupApplications();
	}

    @Test
    public void findAllNodesReturnsJson() {
        log.info("findAllNodesReturnsJson");
        given()
                .when()
                .get("/api/v2/nodes")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("hostname", hasItems("test.devillo.no"));
    }

    @Test
    public void findNodesWithQueryParams() {
        given()
                .queryParam("environmentClass", "u")
                .queryParam("environment", "u1")
                .when()
                .get("/api/v2/nodes")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("hostname", hasItems("test.devillo.no"));
    }

    @Test
    public void findNodeWithLifecycleStatusAlerted() {
        given()
                .queryParam("status", "alerted")
                .when()
                .get("/api/v2/nodes")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("lifecycle.status", hasItem("alerted"));
    }

    @Test
    public void getNodeWithIp() {
        given()

                .queryParam("hostname", "1.2.3.4")
                .when()
                .get("/api/v2/nodes")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("hostname", hasItem("1.2.3.4"));
    }

    @Test
    public void nodeCanBeMappedToMoreThanOneCluster() {
        Environment dev = new Environment("dev", EnvironmentClass.u);

        Application app1Cluster1 = applicationRepository.save(new Application("app1Cluster1"));
        Application app2Cluster1 = applicationRepository.save(new Application("app2Cluster1"));
        Application app1Cluster2 = applicationRepository.save(new Application("app1Cluster2"));

        Node aNode = new Node("dev.devillo.no", "user", "secret");

        Cluster cluster1 = dev.addCluster(new Cluster("cluster1", Domain.Devillo));
        dev.addNode(cluster1, aNode);

        Cluster cluster2 = dev.addCluster(new Cluster("cluster2", Domain.Devillo));
        dev.addNode(cluster2, aNode);

        environmentRepo.save(dev);

        cluster1.addApplication(app1Cluster1);
        cluster1.addApplication(app2Cluster1);
        cluster2.addApplication(app1Cluster2);

        environmentRepo.save(dev);

        given()
                .when()
                .get("/api/v2/nodes/dev.devillo.no")
                .then()
                .statusCode(200)
                .body("applications", hasItems("app1Cluster1", "app2Cluster1", "app1Cluster2"));

    }

    @Test
    public void findNodesByApplicationAndEnviornment() {
        given()
                .queryParam("environment", "u2")
                .queryParam("application", "app1")
                .when()
                .get("/api/v2/nodes")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("hostname", hasItems("anotherNode.devillo.no"));
        ;
    }


    @Test
    public void findNoNodesInQ() {
        given()
                .queryParam("environmentclass", "q")
                .when()
                .get("/api/v2/nodes")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(not(containsString("test.devillo.no")));
    }

    @Test
    public void getNode() {
        given()
                .when()
                .get("/api/v2/nodes/test.devillo.no")
                .then()
                .statusCode(200)
                .body("hostname", equalTo("test.devillo.no"))
                .body("environment", equalTo("u1"))
                .body("environmentclass", equalTo("u"))
                .body("username", equalTo("user"))
                .body("password.ref", notNullValue())
                .body("type", equalTo("wildfly"))
                .body("cluster.name", hasItem("junit"))
                .body("cluster.ref", hasItem(containsString("/u1/clusters/junit")));
    }

    @Test
    public void getNodeByUnknownHostname() {
        given()
                .when()
                .get("/api/v2/nodes/unknown.devillo.no")
                .then()
                .statusCode(404);
    }

    @Test
    public void deleteNodeShouldBeOk() {
        given()
                .when()
                .get("/api/v2/nodes/deleteme.devillo.no")
                .then()
                .statusCode(200);

        given()
                .auth().preemptive().basic("user", "user")
                .when()
                .delete("/api/v2/nodes/deleteme.devillo.no")
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v2/nodes/deleteme.devillo.no")
                .then()
                .statusCode(404);
    }

    @Test
    public void createNodeWithDillDoesNotValidate() {
        given()
                .body("{\"dill\":\"dall\"}")
                .auth().basic("user", "user")
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/nodes")
                .then()
                .statusCode(400)
                .contentType(ContentType.TEXT)
                .body(containsString("Input did not pass validation"))
                .body(containsString("hostname is required"));

    }

    @Test
    public void createNodeShouldBeOk() {

        NodePayload newNode = new NodePayload("created.devillo.no", EnvironmentClass.u, "u1", PlatformType.JBOSS);
        newNode.password = SecretPayload.withValue("password");
        newNode.applications.add("app1");
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(newNode))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/nodes")
                .then()
                .statusCode(201)
                .log().ifError()
                .header("location", containsString("nodes/created.devillo.no"));

        given()
                .when()
                .get("/api/v2/nodes/created.devillo.no")
                .then()
                .statusCode(200);

    }

    @Test
    public void createsNewClusterIfProvided() {

        NodePayload newNode = new NodePayload("created2.devillo.no", EnvironmentClass.u, "u1", PlatformType.JBOSS);

        newNode.cluster.add(new Link("test-cluster"));
        newNode.password = SecretPayload.withValue("password");

        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(newNode))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/nodes")
                .then()
                .statusCode(201)
                .log().ifError()
                .header("location", containsString("nodes/created2.devillo.no"));

        given()
                .when()
                .get("/api/v2/nodes/created2.devillo.no")
                .then()
                .body("cluster.name", hasItem("test-cluster"))
                .statusCode(200);

    }

    @Test
    public void addsNodeToExistingClusterIfProvided() {

        NodePayload newNode = new NodePayload("created3.devillo.no", EnvironmentClass.u, "u1", PlatformType.JBOSS);

        newNode.cluster.add(new Link("test-cluster"));
        newNode.password = SecretPayload.withValue("password");

        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(newNode))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/nodes")
                .then()
                .statusCode(201)
                .log().ifError()
                .header("location", containsString("nodes/created3.devillo.no"));

        given()
                .when()
                .get("/api/v2/nodes/created3.devillo.no")
                .then()
                .body("cluster.name", hasItem("test-cluster"))
                .statusCode(200);

    }

    @Test
    public void generatesClusterNameIfNotProvided() {

        NodePayload newNode = new NodePayload("created4.devillo.no", EnvironmentClass.u, "u1", PlatformType.JBOSS);
        newNode.password = SecretPayload.withValue("password");
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(newNode))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/nodes")
                .then()
                .statusCode(201)
                .log().ifError()
                .header("location", containsString("nodes/created4.devillo.no"));

        given()
                .when()
                .get("/api/v2/nodes/created4.devillo.no")
                .then()
                .body("cluster.name", hasItem(startsWith("cluster")))
                .statusCode(200);

    }

    @Test
    public void createNodeDuplicateShouldThrowError() {

        NodePayload newNode = new NodePayload("test.devillo.no", EnvironmentClass.u, "u1", PlatformType.JBOSS);
        given()
                .auth().basic("user", "user")
                .body(toJson(newNode))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/nodes")
                .then()
                .statusCode(400)
                .body(containsString("Node with hostname test.devillo.no already exists"));
    }

    @Test
    public void createNodeWithMissingFieldsShouldThrowError() {

        NodePayload newNode = new NodePayload();
        given()
                .auth().basic("user", "user")
                .body(toJson(newNode))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/nodes")
                .then()
                .statusCode(400)
                .body(containsString("hostname is required"), containsString("environment is required"), containsString("type is required"));
    }

    @Test
    public void stopNode() {
        NodePayload payload = new NodePayload("changeme.devillo.no", EnvironmentClass.u, "u1", PlatformType.JBOSS);
        payload.lifecycle.status = LifeCycleStatus.STOPPED;
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(payload))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/nodes/changeme.devillo.no")
                .then()
                .log().ifError()
                .statusCode(200);


        given()
                .when()
                .get("/api/v2/nodes/changeme.devillo.no")
                .then()
                .body(containsString("stopped"))
                .statusCode(200);
    }

    @Test
    public void startNode() {
        NodePayload payload = new NodePayload("changeme.devillo.no", EnvironmentClass.u, "u1", PlatformType.JBOSS);
        payload.lifecycle.status = LifeCycleStatus.RUNNING;
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(payload))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/nodes/changeme.devillo.no")
                .then()
                .statusCode(200);
    }

    @Test
    public void checkRevisionWithComment() {
        NodePayload payload = new NodePayload("changeme.devillo.no", EnvironmentClass.u, "u1", PlatformType.JBOSS);
        payload.username = "newUser";
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(payload))
                .header("x-comment", "this is a comment")
                .header("x-onbehalfof", "someone")
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/nodes/changeme.devillo.no")
                .then()
                .statusCode(200);


        given()
                .when()
                .get("/api/v2/nodes/changeme.devillo.no/revisions")
                .then()
                .body("message", hasItem("this is a comment"))
                .body("onbehalfof.id", hasItem("someone"))
                .statusCode(200);
    }

}
