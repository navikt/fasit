package no.nav.aura.fasit.rest;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload;
import no.nav.aura.fasit.rest.model.PortPayload.PortType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import jakarta.inject.Inject;
import java.net.URI;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestInstance(Lifecycle.PER_CLASS)
public class ApplicationInstanceRestTest extends RestTest {
	private static final Logger log = LoggerFactory.getLogger(ApplicationInstanceRestTest.class);
    private static ApplicationInstance fasit_u1;
    private static ApplicationInstance deleteme_u1;

    private static Resource r1;
    private static Resource r2;
    private static Resource r3;
    private static Resource exposedResource;
    
    @Inject
    private ApplicationRepository applicationRepository;
    
    @Inject
    private ApplicationInstanceRepository appInstanceRepository;
    
    @Inject
    private ResourceRepository resourceRepository;
    
    @Inject
    private EnvironmentRepository environmentRepo;

    @BeforeAll
    @Transactional
    public void setUp() throws Exception {
        Application tsys = applicationRepository.save(new Application("tsys"));
        Application gosys = applicationRepository.save(new Application("gosys"));
        Application fasit = applicationRepository.save(new Application("fasit"));
        Application deleteme = applicationRepository.save(new Application("deleteme"));
        Application sys = applicationRepository.save(new Application("sys"));


        Environment u1 = new Environment("u1", EnvironmentClass.u);
        Environment q1 = new Environment("q1", EnvironmentClass.q);
        Environment t1 = new Environment("t1", EnvironmentClass.t);
        Environment t2 = new Environment("t2", EnvironmentClass.t);

        addCluster(t1, tsys, sys);
        addCluster(u1, tsys, gosys, fasit, deleteme);
        addCluster(q1, tsys, gosys, fasit);
        addCluster(t2, gosys, fasit);

        fasit_u1 = appInstanceRepository.findInstanceOfApplicationInEnvironment("fasit", "u1");
        fasit_u1.setLifeCycleStatus(LifeCycleStatus.ALERTED);
        appInstanceRepository.save(fasit_u1);
        deleteme_u1 = appInstanceRepository.findInstanceOfApplicationInEnvironment("deleteme", "u1");

        exposedResource = new Resource("anExposedResource", ResourceType.RestService, new Scope(EnvironmentClass.u));
        exposedResource.putProperty("url", "http://anExposedUrl.no");
        exposedResource.putProperty("description", "such exposed");

        exposedResource = resourceRepository.save(exposedResource);

        r1 = resourceRepository.save(resource("r1"));
        r2 = resourceRepository.save(resource("r2"));
        r3 = resourceRepository.save(resource("r3"));
    }
    
    @AfterAll
    @Transactional
    void tearDown() throws Exception {
    	cleanupEnvironments();
    	cleanupResources();
    	cleanupApplications();
	}


    private static Resource resource(String alias) {
        Resource resource = new Resource(alias, ResourceType.BaseUrl, new Scope(EnvironmentClass.u));
        resource.putPropertyAndValidate("url", "http://something");
        return resource;
    }

    private void addCluster(Environment environment, Application... apps) {
        Domain domain = Domain.getByEnvironmentClass(environment.getEnvClass()).get(0);
        Cluster clusterTestLocal = new Cluster(apps[0].getName() + "Cluster", domain);

        environment.addCluster(clusterTestLocal);
        environment = environmentRepo.saveAndFlush(environment);
        for (Application application : apps) {
            clusterTestLocal.addApplication(application);
        }
        environmentRepo.save(environment);
    }

    @Test
    public void getApplicationInstance() {
        given()
                .pathParam("environment", "u1")
                .pathParam("application", "fasit")
                .when()
                .get("/api/v2/applicationinstances/environment/{environment}/application/{application}")
                .then()
                .statusCode(200)
                .body("application", equalTo("fasit"))
                .body("environment", equalTo("u1"));
    }

    @Test
    public void findApplicationInstanceWithLifecycleStatusAlerted() {
        given()
                .queryParam("status", "alerted")
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("lifecycle.status", hasItem("alerted"))
                .body("application", hasItem("fasit"));
    }

    @Test
    public void findInstanceAll() {
        given()
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(greaterThan(8)))
                .header("total_count", notNullValue());
    }

    @Test
    public void findInstanceByAppName() {
        given()
                .queryParam("application", "fasit")
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(4))
                .body("environment", hasItems("u1", "q1", "t2"));
    }

    @Test
    public void findInstanceByLikeAppName() {
        given()
                .queryParam("application", "sys")
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(7))
                .body("environment", hasItems("t1", "u1", "q1", "t2"));
    }

    @Test
    public void findInstanceByEnvironment() {
        given()
                .queryParam("environment", "t2")
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("application", hasItems("gosys", "fasit"));
    }

    @Test
    public void findInstanceByEnvironmentClass() {
        given()
                .queryParam("environmentclass", "u")
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .body("application", hasItems("fasit", "gosys", "tsys"));
    }

    @Test
    public void findSingleApplication() {
        given()
                .queryParam("environment", "t2")
                .queryParam("application", "fasit")
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("application", hasItem("fasit"))
                .body("environment", hasItem("t2"));
    }

    @Test
    public void findByExactApplicationName() {
        given()
                .pathParam("application", "sys")
                .when()
                .get("/api/v2/applicationinstances/application/{application}")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("application", hasItem("sys"))
                .body("environment", hasItem("t1"));
    }

    @Test
    public void findByExactEnvironmentName() {
        given()
                .pathParam("environment", "t1")
                .when()
                .get("/api/v2/applicationinstances/environment/{environment}")
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("application", hasItems("tsys", "sys"))
                .body("environment", hasItem("t1"));
    }

    @Test
    public void doNotShowUsedAndExposedResourcesWhenUsageFlagIsFalse() {
        given()
                .queryParam("environment", "t2")
                .queryParam("application", "fasit")
                .queryParam("usage", "false")
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .body("usedresources[0]", nullValue())
                .body("exposedresources[0]", nullValue())
                .body("missingresources[0]", nullValue());
    }

    @Test
    public void findInstanceByEnvironmentAndApplicationForUnknown() {
        given()
                .queryParam("environment", "t2")
                .queryParam("application", "unknown")
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    public void getById() {
        given()
                .pathParam("id", fasit_u1.getID())
                .when()
                .get("/api/v2/applicationinstances/{id}")
                .then()
                .statusCode(200)
                .body("application", equalTo("fasit"))
                .body("environment", equalTo("u1"))
                .body("cluster.name", equalTo("tsysCluster"));
    }

    @Test
    public void getApplicationBySelfLink() {
        URI link = findLink("fasit", "u1", "self");
        get(link)
                .then()
                .statusCode(200)
                .body("application", equalTo("fasit"))
                .body("environment", equalTo("u1"));
    }

    @Test
    public void getRevisionsByLink() {
        URI link = findLink("fasit", "t2", "revisions");
        get(link)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("revisiontype", hasItem("add"));
    }

    private URI findLink(String app, String environment, String link) {
        String path = given()
                .queryParam("environment", environment)
                .queryParam("application", app)
                .when()
                .get("/api/v2/applicationinstances")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .extract().path("links.%s[0]", link);
        return URI.create(path);
    }

    @Test
    public void updateApplicationInstanceWithPost() {
        ApplicationInstancePayload app = new ApplicationInstancePayload("fasit", "u1");
        app.version = "1.1";
        app.nodes.add(new ApplicationInstancePayload.NodeRefPayload("host1", 9023, PortType.https));
        app.usedresources.add(new ApplicationInstancePayload.ResourceRefPayload(r1.getID()));
        app.updatedBy = "deployerUser";
        app.exposedresources.add(new ApplicationInstancePayload.ResourceRefPayload(r2.getID()));
        app.missingresources.add(new ApplicationInstancePayload.MissingResourcePayload("missing", ResourceType.WebserviceEndpoint));
        app.appconfig = new ApplicationInstancePayload.AppconfigPayload("<xml>fin xml </xml>");

        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(app))
                .contentType(ContentType.JSON)
                .when()
                //.log().body()
                .post("/api/v2/applicationinstances")
                .then()
                .log().body()
                //.statusCode(200)
                .body("version", equalTo("1.1"))
                .body("nodes", hasSize(1))
                .body("nodes[0]", hasEntry("hostname", "host1"))
                .body("nodes[0].ports", hasSize(1))
                .body("nodes[0].ports[0]", hasEntry("port", 9023))
                .body("usedresources.alias", hasItems("r1"))
                .body("usedresources.revision[0]", notNullValue())
                .body("exposedresources.alias", hasItems("r2"))
                .body("missingresources.alias", hasItems("missing"))
                .body("missingresources.type", hasItems("webserviceendpoint"))
                .body("appconfig.ref", containsString("/appconfig"));

        URI link = findLink("fasit", "u1", "revisions");
        get(link)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$.size", greaterThan(1))
                .body("revisiontype", hasItem("mod"));
    }

    @Test
    //@Disabled
    public void creatingAppInstanceWithExposedResourceThatIsAlreadyExposedByAnotherAppInstanceFails() {

        ApplicationInstancePayload firstAppInstance = new ApplicationInstancePayload("tsys", "u1");
        firstAppInstance.version = "1.2";
        firstAppInstance.exposedresources.add(new ApplicationInstancePayload.ResourceRefPayload(exposedResource.getID()));

        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(firstAppInstance))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/applicationinstances")
                .then()
                // .log().body()
                .statusCode(200);

        ApplicationInstancePayload secondAppInstance = new ApplicationInstancePayload("gosys", "u1");
        secondAppInstance.version = "1.1";
        secondAppInstance.exposedresources.add(new ApplicationInstancePayload.ResourceRefPayload(exposedResource.getID()));

        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(secondAppInstance))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/applicationinstances")
                .then()
                // .log().body()
                .statusCode(400);
    }

    @Test
    public void updateApplicationInstanceWithoutNode() {
        ApplicationInstancePayload app = new ApplicationInstancePayload("fasit", "u1");
        app.version = "1.2";
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(app))
                .contentType(ContentType.JSON)
                .pathParam("id", fasit_u1.getID())
                .when()
                .put("/api/v2/applicationinstances/{id}")
                .then()
                // .log().body()
                .statusCode(200)
                .body("version", equalTo("1.2"))
                .body("application", equalTo("fasit"));
    }

    @Test
    public void callingPostWithExistingApplicationRunsUpdate() {
        ApplicationInstancePayload app = new ApplicationInstancePayload("fasit", "u1");
        app.version = "1.2";
        app.clusterName = "nais";
        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(app))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/applicationinstances/")
                .then()
                // .log().all()
                .statusCode(200)
                .body("version", equalTo("1.2"))
                .body("application", equalTo("fasit"));
    }
    @Test
    public void createApplicationInstanceWithPostWithoutClusterNameFails() {
        ApplicationInstancePayload app = new ApplicationInstancePayload("newApp", "u1");
        app.version = "1.2";
        app.domain = "preprod.local";
        app.environmentClass = EnvironmentClass.t;
        given()
                .auth().basic("user", "user")
                .body(toJson(app))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/applicationinstances/")
                .then()
                // .log().body()
                .statusCode(400);
    }
    @Test
    public void createApplicationInstanceWithPost() {
        ApplicationInstancePayload app = new ApplicationInstancePayload("fasit", "t1");
        app.version = "1.2";
        app.clusterName = "nais";
        app.domain = "test.local";
        app.environmentClass = EnvironmentClass.t;
        given()
                .auth().basic("user", "user")
                .body(toJson(app))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/applicationinstances/")
                .then()
                // .log().body()
                .statusCode(200);
    }

    @Test
    public void updateApplicationInstanceNoAccess() {
        ApplicationInstancePayload app = new ApplicationInstancePayload("fasit", "q1");
        app.version = "1.1";
        app.nodes.add(new ApplicationInstancePayload.NodeRefPayload("host1", 9023, PortType.https));

        given()
                .auth().preemptive().basic("user", "user")
                .body(toJson(app))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/applicationinstances")
                .then()
                .statusCode(403);
    }

    @Test
    public void deleteApplicationInstance() {
        given()
                .auth().preemptive().basic("user", "user")
                .contentType(ContentType.JSON)
                .pathParam("id", deleteme_u1.getID())
                .when()
                .delete("/api/v2/applicationinstances/{id}")
                .then()
                .statusCode(204);
    }
}
