package no.nav.aura.fasit.rest.search;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.rest.RestTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchRepositoryTest extends RestTest {

    // Stable unique hostnames prevent conflicts even if a prior tearDown failed
    private static final String SOME_NODE = "someNode-search-test";
    private static final String OTHER_NODE = "newNode-search-test";

    @Inject
    private JdbcTemplate jdbcTemplate;

    private Resource someResource;
    private List<Long> storedAppInstanceIds = new ArrayList<>();

    @BeforeAll
    public void setUp() {
        jdbcTemplate.update("DELETE FROM clusters_node");
        cleanupResources();
        cleanupEnvironments();
        cleanupApplications();

        Environment someEnvironment = repository.store(new Environment("someEnvironment", EnvironmentClass.u));
        repository.store(new Environment("someMoreEnvironment", EnvironmentClass.u));

        Application notDeployedApplication = repository.store(new Application("notDeployedApplication"));
        Cluster notDeployToCluster = someEnvironment.addCluster(new Cluster("notDeployedToCluster", Domain.Devillo));
        notDeployToCluster.addApplication(notDeployedApplication);

        Application someApplication = repository.store(new Application("someApplication"));
        Application otherApplication = repository.store(new Application("otherApplication"));

        Cluster someCluster = someEnvironment.addCluster(new Cluster("someCluster", Domain.Adeo));
        someCluster.addApplication(someApplication);
        someCluster.addApplication(otherApplication);

        // Nodes added to environment only — not to cluster — to avoid clusters_node FK blocking cascade delete
        someEnvironment.addNode(new Node(SOME_NODE, "", ""));
        someEnvironment.addNode(new Node(OTHER_NODE, "", ""));

        someEnvironment = repository.store(someEnvironment);

        // After store, get the managed cluster from the returned environment — the local
        // someCluster variable is a transient object and does not have its DB id set
        Cluster persistedSomeCluster = someEnvironment.getClusters().stream()
                .filter(c -> "someCluster".equals(c.getName()))
                .findFirst()
                .orElseThrow();

        someResource = new Resource("someResource", ResourceType.BaseUrl, new Scope(EnvironmentClass.p));
        someResource.putProperty("url", "someUrl");
        someResource = repository.store(someResource);

        Resource anotherResource = new Resource("otherResource", ResourceType.Channel, new Scope(EnvironmentClass.t));
        anotherResource.putProperty("name", "someChannel");
        anotherResource.putProperty("queueManager", "queuemanagerone");
        repository.store(anotherResource);

        for (ApplicationInstance applicationInstance : persistedSomeCluster.getApplicationInstances()) {
            applicationInstance.setVersion("69");
            applicationInstance.setAppconfigXml("this is my appconfig. There are many like it, but this one is mine");
            ApplicationInstance savedAppInstance = repository.store(applicationInstance);
            storedAppInstanceIds.add(savedAppInstance.getID());
        }
    }

    @AfterAll
    public void tearDown() {
        jdbcTemplate.update("DELETE FROM clusters_node");
        cleanupResources();
        cleanupEnvironments();
        cleanupApplications();
    }

    @Test
    public void matches() {
        given().when().get("/api/v1/navsearch?q=some&maxcount=10")
            .then().statusCode(HttpStatus.OK.value()).contentType(ContentType.JSON).body("$", hasSize(7));

        given().when().get("/api/v1/navsearch?q=other&maxcount=10")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(3));

        given().when().get("/api/v1/navsearch?q=someE&maxcount=10")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));

        given().when().get("/api/v1/navsearch?q=someA&maxcount=10")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(2));

        given().when().get("/api/v1/navsearch?q=node&maxcount=10")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(2));
    }

    @Test
    public void findById() {
        given()
            .queryParam("q", someResource.getID().toString())
            .queryParam("maxcount", 5)
            .queryParam("type", "RESOURCE")
        .when().get("/api/v1/search")
        .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));

        given()
            .queryParam("q", storedAppInstanceIds.get(0).toString())
            .queryParam("maxcount", 5)
            .queryParam("type", "INSTANCE")
        .when().get("/api/v1/search")
        .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));
    }

    @Test
    public void applicationInstancesThatAreNotYetDeployedAreAlsoReturnedInSearchResults() {
        given()
            .queryParam("q", "someenvironment notDeployedApplication")
            .queryParam("maxcount", 5)
            .queryParam("type", "INSTANCE")
        .when().get("/api/v1/search")
        .then().statusCode(HttpStatus.OK.value())
            .body("$", hasSize(1))
            .body("[0].detailedInfo.version", equalTo("Not deployed"));
    }

    @Test
    public void filterSearch() {
        given().queryParam("q", "some").queryParam("maxcount", 5).queryParam("type", "RESOURCE")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(2));

        given().queryParam("q", "some").queryParam("maxcount", 5).queryParam("type", "ENVIRONMENT")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(2));

        given().queryParam("q", "some").queryParam("maxcount", 5).queryParam("type", "CLUSTER")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));

        given().queryParam("q", "some").queryParam("maxcount", 5).queryParam("type", "APPLICATION")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));

        given().queryParam("q", "some").queryParam("maxcount", 5).queryParam("type", "NODE")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));

        given().queryParam("q", "some").queryParam("maxcount", 5).queryParam("type", "INSTANCE")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));

        given().queryParam("q", "other").queryParam("maxcount", 5).queryParam("type", "APPLICATION")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));

        given().queryParam("q", "other").queryParam("maxcount", 5).queryParam("type", "RESOURCE")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));
    }

    @Test
    public void likeSearchInContentOfResource() {
        given().queryParam("q", "some").queryParam("maxcount", 10).queryParam("type", "RESOURCE")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(2));
    }

    @Test
    public void whenSearchMatchesBothAliasAndPropertiesInTheSameResourceOnlyUniqueResultsAreReturned() {
        List<Integer> ids = given()
            .queryParam("q", "some").queryParam("maxcount", 10).queryParam("type", "RESOURCE")
        .when().get("/api/v1/search")
        .then().statusCode(HttpStatus.OK.value())
            .body("$", hasSize(2))
            .extract().path("id");

        assertEquals(2, ids.stream().distinct().count(), "All IDs are unique in search results");
    }

    @Test
    public void findsResultsForDifferentEntityTypes() {
        given().when().get("/api/v1/navsearch?q=some&maxcount=10")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(7));
    }

    @Test
    public void capsSearchResultsToCorrectSize() {
        given().when().get("/api/v1/navsearch?q=SoMe&maxcount=2")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(2));
    }

    @Test
    public void findsInstanceWhenProvidingMultipleSearchWords() {
        given().when().get("/api/v1/navsearch?q=someenvironment%20s&maxcount=10")
            .then().statusCode(HttpStatus.OK.value())
            .body("$", hasSize(1))
            .body("[0].type", equalTo("instance"))
            .body("[0].name", equalTo("someApplication:69"));
    }

    @Test
    public void nonExistingEnvironmentYieldsEmptySearchResultWhenProvidingMultipleWords() {
        given().when().get("/api/v1/navsearch?q=nonexisting%20blahblah&maxcount=10")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(0));
    }

    @Test
    public void searchInAppConfigWithSqlMetaCharactersDoesNotThrow() {
        given().queryParam("q", "poc_test'").queryParam("maxcount", 10).queryParam("type", "APPCONFIG")
            .when().get("/api/v1/search")
            .then().log().all().statusCode(HttpStatus.OK.value());

        given().queryParam("q", "'; DROP TABLE applicationinstance; --").queryParam("maxcount", 10).queryParam("type", "APPCONFIG")
            .when().get("/api/v1/search")
            .then().log().all().statusCode(HttpStatus.OK.value());

        given().queryParam("q", "test' OR '1'='1").queryParam("maxcount", 10).queryParam("type", "APPCONFIG")
            .when().get("/api/v1/search")
            .then().log().all().statusCode(HttpStatus.OK.value());
        
        
        given().queryParam("q", "other").queryParam("maxcount", 5).queryParam("type", "INSTANCE")
        .when().get("/api/v1/search")
        .then().log().all().statusCode(HttpStatus.OK.value()).body("$", hasSize(1));
    }

    @Test
    public void searchInAppConfigFindsMatchingContent() {
        given().queryParam("q", "appconfig").queryParam("maxcount", 10).queryParam("type", "APPCONFIG")
            .when().get("/api/v1/search")
            .then().statusCode(HttpStatus.OK.value()).body("$", hasSize(2));
    }
}

