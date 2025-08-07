package no.nav.aura.fasit.rest;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.fasit.rest.model.ClusterPayload;
import no.nav.aura.fasit.rest.model.Link;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;

@TestInstance(Lifecycle.PER_CLASS)
public class ClusterRestTest extends RestTest {
	
	@Inject
	private ApplicationRepository applicationRepository;
	
	@Inject
	private EnvironmentRepository environmentRepo;
	
	@Inject
	private NodeRepository nodeRepository;
	

    @BeforeAll
    @Transactional
    public void setUp() throws Exception {
        Application tsys = applicationRepository.save(new Application("tsys"));
        Application gosys = applicationRepository.save(new Application("gosys"));
        Application fasit = applicationRepository.save(new Application("fasit"));
        applicationRepository.saveAll(asList(new Application("app1"), new Application("app2"), new Application("app3")));

        Environment u1 = new Environment("u1", EnvironmentClass.u);
        Environment u2 = new Environment("u2", EnvironmentClass.u);

        Cluster cluster1 = new Cluster("cluster1", Domain.Devillo);
        cluster1.setLifeCycleStatus(LifeCycleStatus.STOPPED);
        Cluster cluster2 = new Cluster("cluster2", Domain.Devillo);
        cluster2.setLoadBalancerUrl("http://loadbalanced.com");
        u1.addCluster(cluster1);
        u1.addCluster(cluster2);
        u2.addCluster(new Cluster("updateCluster", Domain.Devillo));
        u2.addCluster(new Cluster("renameCluster", Domain.Devillo));
        u2.addCluster(new Cluster("deleteCluster", Domain.Devillo));
        u1 = environmentRepo.save(u1);
        u2 = environmentRepo.save(u2);

        cluster1.addApplication(tsys);
        cluster1.addApplication(gosys);
        cluster2.addApplication(fasit);

        u1.addNode(cluster1, nodeRepository.save(new Node("node1.devillo.no", "user", "secret")));
        u1.addNode(cluster1, nodeRepository.save(new Node("node2.devillo.no", "user", "secret")));
        u1.addNode(cluster2, nodeRepository.save(new Node("node3.devillo.no", "user", "secret")));
        u1 = environmentRepo.save(u1);

        u2.addNode(nodeRepository.save(new Node("node10.devillo.no", "user", "secret")));
        u2.addNode(nodeRepository.save(new Node("node11.devillo.no", "user", "secret")));
        u2.addNode(nodeRepository.save(new Node("node12.devillo.no", "user", "secret")));

        environmentRepo.save(u2);
    }
    
    @AfterAll
    void tearDown() throws Exception {
		cleanupEnvironments();
		cleanupApplications();
	}

    @Test
    public void findAllClusters() {

        given()
                .when()
                .get("/api/v2/environments/u1/clusters")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("clustername", hasItems("cluster1", "cluster2"));
    }

    @Test
    public void getClusterByName() {
        given()
                .when()
                .get("/api/v2/environments/u1/clusters/cluster2")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("clustername", equalTo("cluster2"))
                .body("environment", equalTo("u1"))
                .body("environmentclass", equalTo("u"))
                .body("loadbalancerurl", equalTo("http://loadbalanced.com"))
                .body("nodes", hasSize(1))
                .body("nodes.name", hasItems("node3.devillo.no"))
                .body("applications", hasSize(1))
                .body("applications.name", hasItems("fasit"));
    }

    @Test
    public void getClusterThatIsStopped() {
        given()
                .when()
                .get("/api/v2/environments/u1/clusters?status=stopped")
                .then()
                .log().all()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("lifecycle.status", hasItem("stopped"))
                .body("clustername", hasItem("cluster1"));
    }

    @Test
    public void getClusterByNameMultiNodeAndApps() {
        given()
                .when()
                .get("/api/v2/environments/u1/clusters/cluster1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("clustername", equalTo("cluster1"))
                .body("environment", equalTo("u1"))
                .body("environmentclass", equalTo("u"))
                .body("nodes", hasSize(2))
                .body("nodes.name", hasItems("node1.devillo.no", "node2.devillo.no"))
                .body("applications", hasSize(2))
                .body("applications.name", hasItems("gosys", "tsys"));
    }

    @Test
    public void getUnknownCluster() {
        given()
                .when()
                .get("/api/v2/environments/u1/clusters/unknown")
                .then()
                .statusCode(404);
    }

    @Test
    public void createEmptyCluster() {
        ClusterPayload cluster = new ClusterPayload("emptyCluster", Zone.FSS);
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/environments/u2/clusters")
                .then()
                .statusCode(201)
                .header("location", containsString("clusters/emptyCluster"));

        given()
                .when()
                .get("/api/v2/environments/u2/clusters/" + cluster.clusterName)
                .then()
                .statusCode(200)
                .body("nodes", empty())
                .body("applications", empty());
    }

    @Test
    public void createClusterWithAppsAndNodes() {
        ClusterPayload cluster = new ClusterPayload("newCluster", Zone.FSS);
        cluster.applications.add(new Link("app1"));
        cluster.nodes.add(new Link("node10.devillo.no"));
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/environments/u2/clusters")
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/api/v2/environments/u2/clusters/" + cluster.clusterName)
                .then()
                .statusCode(200)
                .body("applications.name", hasItem("app1"))
                .body("nodes.name", hasItems("node10.devillo.no"));
    }

    @Test
    public void createClusterDuplicateShouldFail() {
        ClusterPayload cluster = new ClusterPayload("cluster1", Zone.FSS);
        cluster.nodes.add(new Link("unknown.devillo.no"));
        given()
                .auth().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/environments/u1/clusters")
                .then()
                .statusCode(400)
                .body(containsString("cluster1 already exists"));
    }

    @Test
    public void updateCluster() {
        ClusterPayload cluster = new ClusterPayload("updateCluster", Zone.FSS);
        cluster.nodes.add(new Link("node11.devillo.no"));
        cluster.applications.add(new Link("app3"));
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u2/clusters/updateCluster")
                .then()
                .statusCode(200)
                .body("applications", hasSize(1))
                .body("applications.name", hasItem("app3"))
                .body("nodes", hasSize(1))
                .body("nodes.name", hasItems("node11.devillo.no"));
    }

    @Test
    public void updateClusterReplace() {
        updateCluster();

        ClusterPayload cluster = new ClusterPayload("updateCluster", Zone.FSS);
        cluster.nodes.add(new Link("node12.devillo.no"));
        cluster.applications.add(new Link("app2"));
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u2/clusters/updateCluster")
                .then()
                .statusCode(200)
                .body("applications", hasSize(1))
                .body("applications.name", hasItem("app2"))
                .body("nodes", hasSize(1))
                .body("nodes.name", hasItems("node12.devillo.no"));
    }

    @Test
    public void updateClusterRename() {
        ClusterPayload cluster = new ClusterPayload("newNameCluster", Zone.FSS);
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u2/clusters/renameCluster")
                .then()
                .statusCode(200)
                .body("clustername", equalTo("newNameCluster"));

        given()
                .when()
                .get("/api/v2/environments/u2/clusters/renameCluster")
                .then()
                .statusCode(404);

        given()
                .when()
                .get("/api/v2/environments/u2/clusters/newNameCluster")
                .then()
                .statusCode(200);
    }

    @Test
    public void updateClusterFailsOnUnkownNode() {
        ClusterPayload cluster = new ClusterPayload("cluster1", Zone.FSS);
        cluster.nodes.add(new Link("unknown.devillo.no"));
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u1/clusters/cluster1")
                .then()
                .statusCode(400)
                .body(containsString("unknown.devillo.no is not in environment u1"));
    }

    @Test
    public void nodesCanBeMappedToMultipleClusters() {
        ClusterPayload cluster = new ClusterPayload("cluster1", Zone.FSS);
        cluster.nodes.add(new Link("node3.devillo.no"));
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u1/clusters/cluster1")
                .then()
                .statusCode(200);
    }

    @Test
    public void updateClusterFailsOnUnkownApplication() {
        ClusterPayload cluster = new ClusterPayload("cluster1", Zone.FSS);
        cluster.applications.add(new Link("unknown"));
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u1/clusters/cluster1")
                .then()
                .statusCode(400)
                .body(containsString("Application unknown "));
    }

    @Test
    public void updateClusterFailsOnDuplicateApplicationMapping() {
        ClusterPayload cluster = new ClusterPayload("cluster1", Zone.FSS);
        cluster.applications.add(new Link("fasit"));
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/environments/u1/clusters/cluster1")
                .then()
                .statusCode(400)
                .body(containsString("Application fasit is already "));
    }

    @Test
    public void deleteCluster() {
        given()
                .auth().preemptive().basic("user", "user")
                .when()
                .delete("/api/v2/environments/u2/clusters/deleteCluster")
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v2/environments/u2/clusters/deleteCluster")
                .then()
                .statusCode(404);
    }

}
