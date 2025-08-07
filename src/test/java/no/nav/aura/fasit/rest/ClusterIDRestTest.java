package no.nav.aura.fasit.rest;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.fasit.rest.model.ClusterPayload;
import no.nav.aura.fasit.rest.model.Link;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;

public class ClusterIDRestTest extends RestTest {

    private static Long cluster1Id;
    private static Long cluster2Id;
    private static Long deleteClusterId;

    @BeforeAll
    @Transactional
    public static void setUp() throws Exception {
        ApplicationRepository applicationRepository = jetty.getBean(ApplicationRepository.class);
        Application tsys = applicationRepository.save(new Application("tsys"));
        Application gosys = applicationRepository.save(new Application("gosys"));
        Application fasit = applicationRepository.save(new Application("fasit"));
        applicationRepository.saveAll(asList(new Application("app1"), new Application("app2"), new Application("app3")));

        Environment u1 = new Environment("u1", EnvironmentClass.u);
        Environment u2 = new Environment("u2", EnvironmentClass.u);

        EnvironmentRepository environmentRepo = jetty.getBean(EnvironmentRepository.class);
        Cluster cluster1 = new Cluster("cluster1", Domain.Devillo);
        Cluster cluster2 = new Cluster("cluster2", Domain.Devillo);
        cluster2.setLoadBalancerUrl("http://loadbalanced.com");
        u1.addCluster(cluster1);
        u1.addCluster(cluster2);
        Cluster deleteCluster = new Cluster("deleteCluster", Domain.Devillo);
        u2.addCluster(deleteCluster);
        u1 = environmentRepo.save(u1);
        u2 = environmentRepo.save(u2);

        cluster1.addApplication(tsys);
        cluster1.addApplication(gosys);
        cluster2.addApplication(fasit);

        NodeRepository nodeRepository = jetty.getBean(NodeRepository.class);
        u1.addNode(cluster1, nodeRepository.save(new Node("node1.devillo.no", "user", "secret")));
        u1.addNode(cluster1, nodeRepository.save(new Node("node2.devillo.no", "user", "secret")));
        u1.addNode(cluster2, nodeRepository.save(new Node("node3.devillo.no", "user", "secret")));
        u1 = environmentRepo.save(u1);

        u2.addNode(nodeRepository.save(new Node("node10.devillo.no", "user", "secret")));
        u2.addNode(nodeRepository.save(new Node("node11.devillo.no", "user", "secret")));
        u2.addNode(nodeRepository.save(new Node("node12.devillo.no", "user", "secret")));

        environmentRepo.save(u2);

        cluster1Id = cluster1.getID();
        cluster2Id = cluster2.getID();
        deleteClusterId = deleteCluster.getID();
    }



    @Test
    public void getClusterById() {
        given()
                .when()
                .get("/api/v2/clusters/" + cluster2Id)
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
    public void getClusterByIdMultiNodeAndApps() {
        given()
                .when()
                .get("/api/v2/clusters/" + cluster1Id)
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
    public void getClusterWithNonExistingClusterID() {
        given()
                .when()
                .get("/api/v2/clusters/1234")
                .then()
                .statusCode(404);
    }

    @Test
    public void updateCluster() {
        ClusterPayload cluster = new ClusterPayload("newClusterName", Zone.FSS);
        cluster.nodes.add(new Link("node11.devillo.no"));
        cluster.environment = "u2";
        cluster.applications.add(new Link("app3"));
        given()
                .auth().basic("user", "user")
                .body(toJson(cluster))
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v2/clusters/" + cluster2Id)
                .then()
                .statusCode(200)
                .body("clustername", is("newClusterName"))
                .body("applications", hasSize(1))
                .body("applications.name", hasItem("app3"))
                .body("nodes", hasSize(1))
                .body("nodes.name", hasItems("node11.devillo.no"));
    }

    @Test
    public void deleteCluster() {
        given()
                .auth().basic("user", "user")
                .when()
                .delete("/api/v2/clusters/" + deleteClusterId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v2/clusters/" + deleteClusterId)
                .then()
                .statusCode(404);
    }
}
