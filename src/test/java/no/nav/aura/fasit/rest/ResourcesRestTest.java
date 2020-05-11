package no.nav.aura.fasit.rest;

import com.xebialabs.restito.semantics.Condition;
import com.xebialabs.restito.server.StubServer;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.model.resource.SecurityToken;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.envconfig.rest.RestTest;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload;
import no.nav.aura.fasit.rest.model.ResourcePayload;
import no.nav.aura.fasit.rest.model.ScopePayload;
import no.nav.aura.fasit.rest.model.SecretPayload;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.util.Set;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.*;
import static com.xebialabs.restito.semantics.Condition.basicAuth;
import static com.xebialabs.restito.semantics.Condition.post;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.t;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.u;
import static no.nav.aura.envconfig.model.resource.ResourceType.*;
import static org.hamcrest.Matchers.*;

public class ResourcesRestTest extends RestTest {
    private static Resource dbResource;
    private static Resource mqResouce;
    private static Resource exposedWs;
    private static Environment u1;
    private Resource deleteMe;
    private ApplicationRepository applicationRepository;
    private EnvironmentRepository environmentRepo;
    private ResourceRepository resourceRepo;
    private ApplicationInstanceRepository applicationInstanceRepository;
    private static StubServer vaultServer;

    @BeforeEach
    public void setup() {
        resourceRepo = jetty.getBean(ResourceRepository.class);
        applicationRepository = jetty.getBean(ApplicationRepository.class);
        applicationInstanceRepository = jetty.getBean(ApplicationInstanceRepository.class);
        Application myapp = applicationRepository.save(new Application("myapp"));
        Application myOtherApp = applicationRepository.save(new Application("myOtherApp"));

        environmentRepo = jetty.getBean(EnvironmentRepository.class);
        Application fasit = applicationRepository.save(new Application("fasit"));

        u1 = new Environment("u1", u);
        u1.addCluster(new Cluster(fasit.getName() + "Cluster", Domain.fromFqdn("devillo.no")));
        u1 = environmentRepo.saveAndFlush(u1);

        Environment t1 = new Environment("t1", t);

        Domain domain = Domain.fromFqdn("test.local");
        Cluster cluster = new Cluster(fasit.getName() + "Cluster", domain);
        t1.addCluster(cluster);
        t1 = environmentRepo.saveAndFlush(t1);


        Resource db = new Resource("myDB", DataSource, new Scope(u).domain(Domain.Devillo).environment(u1));
        db.putProperty("url", "jdbc:url");
        db.putProperty("username", "user");
        db.putProperty("oemEndpoint", "test");
        db.putProperty("onsHosts", "test:6200,test1:6200");
        db.putSecretWithValue("password", "secret");
        db.setLifeCycleStatus(LifeCycleStatus.ALERTED);
        dbResource = resourceRepo.save(db);

        Resource queue = new Resource("myQueue", Queue, new Scope(t).domain(Domain.TestLocal).environment(t1));
        queue.putProperty("queueName", "QA.MY_QUEUE");
        queue.putProperty("queueManager", "MYQUEUEMANAGER");
        queue.setLifeCycleStatus(LifeCycleStatus.STOPPED);
        mqResouce = resourceRepo.save(queue);

        Resource queue2 = new Resource("myOtherQueue", Queue, new Scope(t).application(myapp));
        queue2.putProperty("queueName", "QA.MY_OTHER_QUEUE");
        queue2.putProperty("queueManager", "MYQUEUEMANAGER");
        resourceRepo.save(queue2);

        exposedWs = new Resource("myExposedService", WebserviceEndpoint, new Scope(t).environment(t1));
        exposedWs.putProperty("endpointUrl", "http://thisIsMyUrl.com");
        exposedWs.putProperty("wsdlUrl", "http://thisIsMyWsdl.com");
        exposedWs.putProperty("securityToken", SecurityToken.SAML.toString());
        exposedWs.putProperty("description", "this is a description. There are many like it. but this one is mine");
        exposedWs = resourceRepo.save(exposedWs);

        deleteMe = new Resource("deleteMe", BaseUrl, new Scope(u));

        deleteMe.putProperty("url", "babombiboom");
        deleteMe = resourceRepo.save(deleteMe);

        ApplicationInstance applicationInstance = cluster.addApplication(fasit);

        ResourceReference mqref = new ResourceReference(mqResouce, 0L);
        Set<ResourceReference> resourceReferences = applicationInstance.getResourceReferences();
        resourceReferences.add(mqref);

        ExposedServiceReference esr = new ExposedServiceReference(exposedWs, 0L);
        Set<ExposedServiceReference> exposedServices = applicationInstance.getExposedServices();

        exposedServices.add(esr);
        environmentRepo.save(t1);

    }


    @AfterEach
    public void cleanup() {
        ResourceRepository resourceRepo = jetty.getBean(ResourceRepository.class);
        ApplicationRepository applicationRepository = jetty.getBean(ApplicationRepository.class);
        EnvironmentRepository environmentRepo = jetty.getBean(EnvironmentRepository.class);
        environmentRepo.deleteAll();
        applicationRepository.deleteAll();
        resourceRepo.deleteAll();
    }

    @BeforeAll
    public static void setupAll() {
        vaultServer = new StubServer().run();
        System.setProperty("vault.url", "http://localhost:" + vaultServer.getPort());
        System.setProperty("vault.token", "dummy");
    }

    @AfterAll
    public static void afterAll() {
        System.clearProperty("vault.url");
        System.clearProperty("vault.token");
        vaultServer.stop();
    }

    /*@Test
    public void findAllResources() {
        System.out.println("dbResource = " + jetty.getBean(ResourceRepository.class).findAll().size());
        given()
                .when()
                .get("/api/v2/resources/")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(5))
                .header("total_count", equalTo("5"));
    }
*/

  /*  @Test
    public void findResourceById() {
        given()
                .when()
                .get("/api/v2/resources/" + dbResource.getID())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("alias", equalTo("myDB"))
                .body("scope.environmentclass", equalTo("u"))
                .body("scope.environment", equalTo("u1"))
                .body("scope.zone", equalTo("fss"));
    }*/

    /*@Test
    public void findByIdAlsoShowsUsedByApplications() {
        given()
                .when()
                .get("/api/v2/resources/" + mqResouce.getID())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("alias", equalTo("myQueue"))
                .body("usedbyapplications.application", hasItem("fasit"))
                .body("usedbyapplications", hasSize(1));
    }*/

    /*@Test
    public void findByLikeAlias() {
        given()
                .when()
                .get("/api/v2/resources?alias=my")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("usedbyapplications", hasSize(4));
    }*/

    /*@Test
    public void findByIdAlsoShowsExposedByApplication() {
        given()
                .when()
                .get("/api/v2/resources/" + exposedWs.getID())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("alias", equalTo("myExposedService"))
                .body("exposedby.application", equalTo("fasit"));
    }*/


    /*@Test
    public void findResourceByAlias() {
        given()
                .when()
                .get("/api/v2/resources?alias=myQueue")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("alias", hasItem("myQueue"));
    }*/

    /*@Test
    public void findStoppedResources() {
        given()
                .when()
                .get("/api/v2/resources?status=stopped")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("alias", hasItem("myQueue"))
                .body("lifecycle.status", hasItem("stopped"));
    }*/

    /*@Test
    public void findAlertedResources() {
        given()
                .when()
                .get("/api/v2/resources?status=alerted")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("alias", hasItem("myDB"))
                .body("lifecycle.status", hasItem("alerted"));

    }*/

    @Test
    public void findResourceByEnvironmentClass() {
        given()
                .when()
                .get("/api/v2/resources?environmentclass=t")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(3))
                .body("alias", hasItems("myQueue", "myOtherQueue"));
    }

    /*@Test
    public void findResourceByEnvironment() {
        given()
                .when()
                .get("/api/v2/resources?environment=t1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("alias", hasItem("myQueue"));
    }*/

    /*@Test
    public void findResourceByApplication() {
        given()
                .when()
                .get("/api/v2/resources?application=myapp")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("alias", hasItem("myOtherQueue"));
    }*/

    /*@Test
    public void findResourceByEnvironmentClassAndApplication() {
        given()
                .when()
                .get("/api/v2/resources?environmentclass=t&application=myapp")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("alias", hasItem("myOtherQueue"));
    }*/

    /*@Test
    public void QueryParamZoneWithoutEnvironmentOrEnvClassGivesError() {
        given()
                .when()
                .get("/api/v2/resources?zone=fss")
                .then()
                .statusCode(400);
    }*/

    /*@Test
    public void findResourceByType() {
        given()
                .when()
                .get("/api/v2/resources?type=queue")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("alias", hasItems("myQueue", "myOtherQueue"));
    }*/

    /*@Test
    public void getRevisionsByLink() {
        ResourceRepository resourceRepo = jetty.getBean(ResourceRepository.class);
        dbResource.setAlias("updatedAlias");
        resourceRepo.save(dbResource);

        URI link = findLink(dbResource, "revisions");
        get(link)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("revisiontype", hasItems("add", "mod"));
    }*/

    /*@Test
    public void getResourceTypes() {
        given()
                .when()
                .get("/api/v2/resources/types")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("type", hasItems("DataSource", "BaseUrl"));
    }*/

    /*@Test
    public void createResource() {
        ResourcePayload newResourcePayload = new ResourcePayload();
        newResourcePayload.type = BaseUrl;
        newResourcePayload.alias = "newresource";
        newResourcePayload.addProperty("url", "http://myurl.com");
        newResourcePayload.scope = new ScopePayload().environmentClass(u).environment("u1").application("fasit");

        createResource(newResourcePayload)
                .then()
                .statusCode(201)
                .header("location", containsString("resources"));

        given()
                .when()
                .get("/api/v2/resources/?alias=newresource")
                .then()
                .statusCode(200)
                .body("type", hasItem("baseurl"))
                .body("alias", hasItem("newresource"))
                .body("properties.url", hasItem("http://myurl.com"))
                .body("scope.environmentclass", hasItem("u"))
                .body("scope.environment", hasItem("u1"))
                .body("scope.application", hasItem("fasit"));
    }
*/
/*    @Test
    public void createResourceWithVaultSecret() {
        ResourcePayload newResourcePayload = new ResourcePayload();
        newResourcePayload.type = DataSource;
        newResourcePayload.alias = "myTestDS";
        newResourcePayload.scope = new ScopePayload().environmentClass(u).environment("u1").application("fasit");
        newResourcePayload.addProperty("url", "jdbc:thin:foo/bar");
        newResourcePayload.addProperty("username", "testusername");
        newResourcePayload.secrets.put("password", SecretPayload.withVaultPath("db/creds/foobar/password"));

        Response response = createResource(newResourcePayload);
        String resourceUrl = response.getHeader("Location");

        String passwordRef = given()
                .when()
                .get(resourceUrl)
                .getBody().path("secrets.password.ref");

        whenHttp(vaultServer)
                .match(
                        Condition.get("/v1/auth/token/lookup-self")
                )
                .then(ok(), stringContent("{\"data\":{\"display_name\":\"hello\", \"policies\":[]}}"), contentType("application/json"));
        whenHttp(vaultServer)
                .match(
                        Condition.get("/v1/db/creds/foobar")
                )
                .then(ok(), stringContent("{\"data\":{\"data\": {\"password\": \"donaldduck\"}, \"metadata\": {}}}"), contentType("application/json"));

        given()
            .when()
            .auth().basic("user", "user")
            .get(passwordRef)
            .then()
            .statusCode(200)
            .contentType("text/plain")
            .body(equalTo("donaldduck"));
    }*/

    /*@Test
    public void createResourceWithWhitespaceInAlias() {
        ResourcePayload newResourcePayload = new ResourcePayload();
        newResourcePayload.type = BaseUrl;
        newResourcePayload.alias = " newresource ";
        newResourcePayload.addProperty("url", "http://myurl.com");
        newResourcePayload.scope = new ScopePayload().environmentClass(u).environment("u1").application("fasit");

        createResource(newResourcePayload)
                .then()
                .statusCode(201)
                .header("location", containsString("resources"));

        given()
                .when()
                .get("/api/v2/resources/?alias=newresource")
                .then()
                .statusCode(200)
                .body("alias", hasItem("newresource"));
    }
*/
    private Response registerDeployment(ApplicationInstancePayload payload) {
        String s = toJson(payload);
        return given()
                .auth().basic("user", "user")
                .body(toJson(payload))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/applicationinstances");
    }

    /*@Test
    public void createResourceWillFailWhenMissingRequiredProperties() {
        ResourcePayload invalidResource = new ResourcePayload();
        invalidResource.alias = "invalidresource";
        invalidResource.type = BaseUrl;
        invalidResource.scope = new ScopePayload().environmentClass(u);

        createResource(invalidResource)
                .then()
                .statusCode(400)
                .body(containsString("Missing required key in properties: url"));
    }
*/
  /*  @Test
    public void createResourceWillFailWhenInvalidEnvironmentName() {
        ResourcePayload invalidResource = new ResourcePayload();
        invalidResource.alias = "invalidresource";
        invalidResource.type = BaseUrl;
        invalidResource.scope = new ScopePayload().environmentClass(u).environment("notreal");

        createResource(invalidResource)
                .then()
                .statusCode(400)
                .body(containsString("Environment notreal does not exist"));
    }*/

    /*@Test
    public void createResourceWillFailWhenInvalidApplicationtName() {
        ResourcePayload invalidResource = new ResourcePayload();
        invalidResource.alias = "invalidresource";
        invalidResource.type = BaseUrl;
        invalidResource.scope = new ScopePayload().environmentClass(u).application("notreal");

        createResource(invalidResource)
                .then()
                .statusCode(400)
                .body(containsString("Application notreal does not exist"));
    }*/

    /*@Test
    public void duplicateResourceWillFail() {
        ResourcePayload duplicate = createResourcePayload();

        createResource(duplicate)
                .then()
                .statusCode(201)
                .header("location", containsString("resources"));

        createResource(duplicate)
                .then()
                .statusCode(400)
                .body(containsString("Duplicate resource"));
    }*/

    /*@Test
    public void deleteResourceShouldBeOk() {
        long id = deleteMe.getID();

        given()
                .when()
                .get("/api/v2/resources/" + id)
                .then()
                .statusCode(200);

        given()
                .auth().basic("user", "user")
                .when()
                .delete("/api/v2/resources/" + id)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v2/resources/" + id)
                .then()
                .statusCode(404);
    }*/

    /*@Test
    public void deleteResourceThatIsExposedByAnApplicationShouldBeOk() {

        ResourcePayload newResourcePayload = new ResourcePayload();
        newResourcePayload.type = RestService;
        newResourcePayload.alias = "arestservicetodelete";
        newResourcePayload.addProperty("url", "http://myurl.com");
        newResourcePayload.addProperty("description", "blah");
        newResourcePayload.scope = new ScopePayload().environmentClass(u).environment("u1");

        Response newResourceResponse = createResource(newResourcePayload);
        String[] locationUrlParts = newResourceResponse.header("Location").split("/");
        Long newResourceId = Long.valueOf(locationUrlParts[locationUrlParts.length - 1]);

        ApplicationInstancePayload app = new ApplicationInstancePayload("myapp", "u1");
        app.version = "69";
        app.clusterName = "fasitCluster";
        app.exposedresources.add(new ApplicationInstancePayload.ResourceRefPayload(newResourceId));



        registerDeployment(app);
        given()
                .auth()
                .basic("user", "user")
                .when()
                .delete("/api/v2/resources/" + newResourceId)
                .then()
                .statusCode(204);
    }*/

    /*@Test
    public void deleteResourceThatIsExposedByMultipleApplicationsShouldBeOk() {

        ResourcePayload newResourcePayload = new ResourcePayload();
        newResourcePayload.type = RestService;
        newResourcePayload.alias = "arestservicetodelete";
        newResourcePayload.addProperty("url", "http://myurl.com");
        newResourcePayload.addProperty("description", "blah");
        newResourcePayload.scope = new ScopePayload().environmentClass(u).environment("u1");

        Response newResourceResponse = createResource(newResourcePayload);
        String[] locationUrlParts = newResourceResponse.header("Location").split("/");
        Long newResourceId = Long.valueOf(locationUrlParts[locationUrlParts.length - 1]);

        ApplicationInstancePayload app = new ApplicationInstancePayload("myapp", "u1");
        app.version = "69";
        app.clusterName = "fasitCluster";
        app.exposedresources.add(new ApplicationInstancePayload.ResourceRefPayload(newResourceId));

        ApplicationInstancePayload otherApp = new ApplicationInstancePayload("myOtherApp", "u1");
        app.version = "69";
        app.clusterName = "fasitCluster";
        app.exposedresources.add(new ApplicationInstancePayload.ResourceRefPayload(newResourceId));


        registerDeployment(app);
        registerDeployment(otherApp);
        given()
                .auth()
                .basic("user", "user")
                .when()
                .delete("/api/v2/resources/" + newResourceId)
                .then()
                .statusCode(204);
    }

    @Test
    public void updateResourceDoesNotCreateANewResource() {
        dbResource.getID();

        ResourcePayload updatedPayload = new ResourcePayload();
        updatedPayload.scope = new ScopePayload().environmentClass(EnvironmentClass.u).application("fasit");
        updatedPayload.type = ResourceType.DataSource;
        updatedPayload.alias = "updatedAlias";
        updatedPayload.properties.put("url", "updatedurl");
        updatedPayload.properties.put("username", "updatedusername");
        updatedPayload.secrets.put("password", SecretPayload.withValue("updatedpassword"));


        updateResource(updatedPayload, dbResource.getID())
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/api/v2/resources?alias=updatedAlias")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1));

    }

    @Test
    public void updateNonExistingResourceFails() {

    }

    @Test
    public void updateResourceTypeFails() {

    }*/

    private Response createResource(ResourcePayload resource) {
        return given()
                .auth().basic("user", "user")
                .body(toJson(resource))
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v2/resources");
    }

    private Response updateResource(ResourcePayload updatedPayload, Long resourceId) {
        return given()
                .auth().basic("user", "user")
                .body(toJson(updatedPayload))
                .contentType(ContentType.JSON)
                .pathParam("id", resourceId)
                .when()
                .put("/api/v2/resources/{id}");
    }

    private ResourcePayload createResourcePayload() {
        ResourcePayload resourcePayload = new ResourcePayload();
        resourcePayload.type = BaseUrl;
        resourcePayload.alias = "newresource";
        resourcePayload.addProperty("url", "http://myurl.com");
        resourcePayload.scope = new ScopePayload().environmentClass(u);
        return resourcePayload;
    }

    private URI findLink(Resource resource, String link) {
        String path = given()
                .when()
                .get("/api/v2/resources/" + resource.getID())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().path("links.%s", link);
        return URI.create(path);

    }
}
