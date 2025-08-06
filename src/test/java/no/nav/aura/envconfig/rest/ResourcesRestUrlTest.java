package no.nav.aura.envconfig.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.restassured.http.ContentType;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.client.rest.PropertyElement;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.util.SerializableFunction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.http.HttpStatus;

import jakarta.persistence.NoResultException;
import java.io.File;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static no.nav.aura.envconfig.util.TestHelper.assertAndGetSingleOrNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(Lifecycle.PER_CLASS)
public class ResourcesRestUrlTest extends RestTest {

    private static Environment environment;
    private static Application application;

    @BeforeAll
    public void setUp() throws Exception {
        application = repository.store(new Application("app"));
        environment = repository.store(new Environment("myEnv", EnvironmentClass.u));
        Resource db = new Resource("myDB", ResourceType.DataSource, new Scope(EnvironmentClass.u).domain(Domain.Devillo).envName("myEnv"));
        db.putPropertyAndValidate("url", "jdbc:url");
        db.putPropertyAndValidate("username", "user");
        db.putPropertyAndValidate("oemEndpoint", "test");
        db.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        db.putSecretAndValidate("password", "secret");
        repository.store(db);
    }
    
    @AfterAll
    void tearDown() {
		cleanupEnvironments();
		cleanupApplications();
		cleanupResources();
	}

    @Test
    public void resourceBestMatch_shouldBeCaseInsensitive() {
        String alias = "aCamelCasedBaseUrl";
        String envName = "MyEnv";
        Resource resource = new Resource(alias, ResourceType.BaseUrl, new Scope(new Environment(envName, EnvironmentClass.u)));
        resource.getProperties().put("url", "https://myurl.com");
        repository.store(new Application("tulleAPP"));
        repository.store(resource);
        expect().statusCode(HttpStatus.NOT_FOUND.value()).when().get("/conf/resources/bestmatch?envClass=P&domain=devillo.no&envName=" + envName + "&alias=" + alias + "&type=BaseUrl&app=tulleapp");
        expect().statusCode(HttpStatus.OK.value()).when().get("/conf/resources/bestmatch?envClass=U&domain=devillo.no&envName=" + envName.toLowerCase() + "&alias=" + alias.toUpperCase() + "&type=BaseUrl&app=tulleapp");
        expect().statusCode(HttpStatus.OK.value()).when().get("/conf/resources/bestmatch?envClass=u&domain=devillo.no&envName=" + envName.toUpperCase() + "&alias=" + alias.toUpperCase() + "&type=BaseUrl&app=TULLEapp");
    }

    @Test
    public void findResoureResourceWithoutAcceptHeaderReturnsXml() {
        expect().statusCode(HttpStatus.OK.value())
                .body(containsString("jdbc:url"))
                .contentType(ContentType.XML)
                .when()
                .get("/conf/resources?envClass=u&alias=myDB&type=DataSource");
    }

    @Test
    public void findResoureResourceReturnsJson() {
        given()
                .header("accept", "application/json")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .body(containsString("jdbc:url"))
                .contentType(ContentType.JSON)
                .when()
                .get("/conf/resources?envClass=u&alias=myDB&type=DataSource");
    }

    @Test
    public void searchResourceShouldReturnStoppedResources() {
        Resource stoppedRes = new Resource("myDB", ResourceType.DataSource, new Scope(EnvironmentClass.u).domain(Domain.Devillo).envName("otherEnv"));
        stoppedRes.putPropertyAndValidate("url", "jdbc:stopped");
        stoppedRes.putPropertyAndValidate("username", "user");
        stoppedRes.putPropertyAndValidate("oemEndpoint", "test");
        stoppedRes.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        stoppedRes.putSecretAndValidate("password", "secret");
        stoppedRes.changeStatus(LifeCycleStatus.STOPPED);
        repository.store(stoppedRes);
        expect().statusCode(HttpStatus.OK.value())
                .body(containsString("jdbc:url"))
                .body(containsString("jdbc:stopped"))
                .when()
                .get("/conf/resources?envClass=u&alias=myDB&type=DataSource");
    }

    @Test
    public void searchResourceAndReturnOnlyOneUniqueWithSameAlias() {
        Resource dbWithoutDbName = new Resource("myDB", no.nav.aura.envconfig.model.resource.ResourceType.DataSource, new Scope(EnvironmentClass.u).domain(Domain.Devillo));
        dbWithoutDbName.putPropertyAndValidate("url", "jdbc:url2");
        dbWithoutDbName.putPropertyAndValidate("username", "user");
        dbWithoutDbName.putPropertyAndValidate("oemEndpoint", "test");
        dbWithoutDbName.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        dbWithoutDbName.putSecretAndValidate("password", "secret");
        repository.store(dbWithoutDbName);
        expect().statusCode(HttpStatus.OK.value())
                .body(not(containsString("jdbc:url2")))
                .body(containsString("jdbc:url"))
                .when()
                .get("/conf/resources?bestmatch=true&envClass=u&alias=myDB&type=DataSource&envName=myEnv");
    }

    @Test
    public void searchResourceWithoutEnvClass() {
        expect().statusCode(HttpStatus.OK.value())
                .body(containsString("jdbc:url"))
                .when()
                .get("/conf/resources?envName=myEnv&alias=myDB&type=DataSource");
    }

    @Test
    public void bestMatchResource() {
        expect().statusCode(HttpStatus.OK.value())
                .body(containsString("jdbc:url"))
                .when()
                .get("/conf/resources/bestmatch?envClass=u&domain=devillo.no&envName=myEnv&app=app&alias=myDB&type=DataSource");
    }

    @Test
    public void bestMatchResourceNotFound() {
        expect().statusCode(HttpStatus.NOT_FOUND.value())
                .body(containsString("with alias unknownDB"))
                .when()
                .get("/conf/resources/bestmatch?envClass=u&domain=devillo.no&envName=myEnv&app=app&alias=unknownDB&type=DataSource");
    }

    @Test
    public void findingNonExistingResourceWithBestMatchMode_givesEmptyResourceList() {
        expect().statusCode(HttpStatus.OK.value())
                .body(containsString("<collection/>"))
                .when()
                .get("/conf/resources?envClass=u&domain=devillo.no&envName=myEnv&app=myapp&alias=unknownDB&type=DataSource&bestmatch=true");
    }

    @Test
    public void findingNonExistingResourceWithoutBestMatchMode_givesEmptyResourceList() {
        expect().statusCode(HttpStatus.OK.value())
                .body(containsString("<collection/>"))
                .when()
                .get("/conf/resources?bestmatch=false&envClass=u&domain=devillo.no&envName=myEnv&app=myapp&alias=unknownDB&type=DataSource");
    }

    @Test
    public void putResourceOK() {
        given().multiPart("alias", "newAlias")
                .multiPart("type", "Credential")
                .multiPart("scope.environmentclass", "u")
                .multiPart("scope.environmentname", "")
                .multiPart("scope.domain", "devillo.no")
                .multiPart("scope.application", "")
                .multiPart("username", "user")
                .multiPart("password", "secret")
                .multiPart("keystorepassword", "keystoresecret")
                .multiPart("keystore.filename", "file.jks")
                .multiPart("keystore.file", "keystore.jks", "dilldalldull".getBytes())
                .multiPart("applicationCertificateAlias", "certAlias")
                .auth().preemptive().basic("prodadmin", "prodadmin")
                .queryParam("entityStoreComment", "mycomment")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .when()
                .put("/conf/resources");

        ResourceElement resource = checkResource("newAlias", ResourceType.Credential, true);
        FasitRevision<Resource> headrevision = getHeadrevision(Resource.class, resource.getId());
        assertEquals("mycomment", headrevision.getMessage());
    }

    @Test
    public void putResourceWithoutAccess() {

        given().multiPart("alias", "newAliasinProd")
                .multiPart("type", "BaseUrl")
                .multiPart("scope.environmentclass", "p")
                .multiPart("url", "http://something")
                .auth().preemptive().basic("user", "user")
                .expect()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .when()
                .put("/conf/resources");
    }

    @Test
    public void deleteResource() {
        Assertions.assertThrows(NoResultException.class, () -> {
            Resource newResource = new Resource("deleteme", no.nav.aura.envconfig.model.resource.ResourceType.BaseUrl, new Scope(EnvironmentClass.u));
            newResource.putPropertyAndValidate("url", "http://someurl");
            Resource resource = repository.store(newResource);
            given()
                    .auth().preemptive().basic("prodadmin", "prodadmin")
                    .expect()
                    .log().all()
                    .statusCode(HttpStatus.NO_CONTENT.value())
                    .when()
                    .delete("/conf/resources/{id}", resource.getID());
            repository.getById(Resource.class, resource.getID());
        });
    }

    @Test
    public void deleteResourceNoAccess() {
        Resource newResource = new Resource("deletemeImInProd", no.nav.aura.envconfig.model.resource.ResourceType.BaseUrl, new Scope(EnvironmentClass.p));
        newResource.putPropertyAndValidate("url", "http://someurl");
        Resource resource = repository.store(newResource);
        given()
                .auth().preemptive().basic("user", "user")
                .expect()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .when()
                .delete("/conf/resources/{id}", resource.getID());
    }

    @Test
    public void updateResource() {
        Resource newResource = new Resource("updateMe", no.nav.aura.envconfig.model.resource.ResourceType.BaseUrl, new Scope(EnvironmentClass.u));
        newResource.putPropertyAndValidate("url", "http://someurl");
        Resource resource = repository.store(newResource);
        given()
                .multiPart("url", "http://newUrl")
                .auth().preemptive().basic("prodadmin", "prodadmin")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .body(containsString("newUrl"))
                .when()
                .post("/conf/resources/{id}", resource.getID());
    }

    @Test
    public void updateResourceChangeStatus() {
        Resource newResource = new Resource("updateMe2", no.nav.aura.envconfig.model.resource.ResourceType.BaseUrl, new Scope(EnvironmentClass.u));
        newResource.putPropertyAndValidate("url", "http://someurl");
        Resource resource = repository.store(newResource);
        given()
                .multiPart("lifeCycleStatus", "STOPPED")
                .auth().preemptive().basic("prodadmin", "prodadmin")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .body(containsString("<lifeCycleStatus>STOPPED"))
                .when()
                .post("/conf/resources/{id}", resource.getID());
    }

    @Test
    public void updateResourceSetAdgroup() {
        Resource newResource = new Resource("updateMe3", no.nav.aura.envconfig.model.resource.ResourceType.BaseUrl, new Scope(EnvironmentClass.u));
        newResource.putPropertyAndValidate("url", "http://someurl");
        Resource resource = repository.store(newResource);
        given()
                .multiPart("accessAdGroup", "somegroup")
                .auth().preemptive().basic("prodadmin", "prodadmin")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .body(containsString("<accessAdGroup>somegroup"))
                .when()
                .post("/conf/resources/{id}", resource.getID());
    }

    @Test
    public void updateResourceNoAccess() {
        Resource newResource = new Resource("updatemeinprod", no.nav.aura.envconfig.model.resource.ResourceType.BaseUrl, new Scope(EnvironmentClass.p));
        newResource.putPropertyAndValidate("url", "http://someurl");
        Resource resource = repository.store(newResource);
        given()
                .multiPart("url", "http://newUrl")
                .auth().preemptive().basic("user", "user")
                .expect()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .when()
                .post("/conf/resources/{id}", resource.getID());
    }

    private ResourceElement checkResource(String alias, ResourceType resourceType, boolean expect) {
        List<ResourceElement> list = expect()
                .statusCode(HttpStatus.OK.value())
                .when()
                .get("/conf/resources?envClass=u&alias=" + alias)
                .xmlPath().getList("collection.resource", ResourceElement.class);

        if (expect) {
            assertEquals(1, list.size());
            ResourceElement resource = list.iterator().next();
            assertEquals(alias, resource.getAlias());
            assertEquals(resourceType.name(), resource.getType().name());
        } else {
            assertEquals(0, list.size());
        }
        return assertAndGetSingleOrNull(list);
    }

    @Test
    @Disabled("Denne feiler på jenkins med broken pipe av en eller annen grunn")
    public void putResourceMissingType() {
        given().multiPart("alias", "newAlias")
                .multiPart("type", "UnknownType")
                .multiPart("scope.environmentclass", "u")
                .multiPart("scope.domain", "devillo.no")
                .auth().basic("prodadmin", "prodadmin")
                .expect()
                .statusCode(HttpStatus.BAD_REQUEST.value()).when()
                .put("/conf/resources");
    }

    @Test
    @Disabled
    public void putResourceCertificate() {
        checkResource("myCertificate", ResourceType.Certificate, false);

        File file = new File(getClass().getResource("/app-config.xml").getPath());
        String alias = "myCertificate";
        given().multiPart("alias", alias)
                .multiPart("type", ResourceType.Certificate.name())
                .multiPart("scope.environmentclass", environment.getEnvClass())
                .multiPart("scope.environmentname", environment.getName())
                .multiPart("scope.domain", Domain.Devillo.getFqn())
                .multiPart("scope.application", application.getName())
                .multiPart("keystorealias", "ali")
                .multiPart("keystorepassword", "keystoresecret")
                .multiPart("keystore.filename", "keystore.jks")
                .multiPart("keystore.file", file)
                .auth().preemptive().basic("prodadmin", "prodadmin")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .when()
                .put("/conf/resources");

        ResourceElement resource = checkResource("myCertificate", ResourceType.Certificate, true);
        String keystoreUri = index(resource).get("keystore").getRef().toString();
        byte[] fileByteArray = given()
                .expect()
                .statusCode(HttpStatus.OK.value())
                .when().get(keystoreUri).asByteArray();
        assertEquals(file.length(), fileByteArray.length);
    }

    @Test
    public void putBaseUrlResource() {
        checkResource("myBaseUrl", ResourceType.BaseUrl, false);

        String alias = "myBaseUrl";
        String url = "http://herogder";
        given().multiPart("alias", alias)
                .multiPart("scope.environmentclass", environment.getEnvClass())
                .multiPart("type", ResourceType.BaseUrl.name())
                .multiPart("scope.environmentname", environment.getName())
                .multiPart("scope.domain", Domain.Devillo.getFqn())
                .multiPart("scope.application", application.getName())
                .multiPart("url", url)
                .auth().preemptive().basic("prodadmin", "prodadmin")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .when()
                .put("/conf/resources");

        ResourceElement resource = checkResource("myBaseUrl", ResourceType.BaseUrl, true);
        assertEquals(url, index(resource).get("url").getValue());
    }

    @Test
    @Deprecated
    // Fjernes n�r nytt api er tatt i bruk over alt
    public void putBaseUrlResourceDeprecatedAPi() {
        String alias = "myBaseUrl2";
        String url = "http://herogder";
        given().multiPart("alias", alias)
                .multiPart("scope.environmentclass", environment.getEnvClass())
                .multiPart("scope.environmentname", environment.getName())
                .multiPart("scope.domain", Domain.Devillo.getFqn())
                .multiPart("scope.application", application.getName())
                .multiPart("url", url)
                .auth().preemptive().basic("prodadmin", "prodadmin")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .when()
                .put("/conf/resources/" + ResourceType.BaseUrl);

        ResourceElement resource = checkResource("myBaseUrl2", ResourceType.BaseUrl, true);
        assertEquals(url, index(resource).get("url").getValue());
    }

    @Test
    public void putDeploymentManagerResource() {
        String alias = "myDeploymentManager";
        checkResource("myDeploymentManager", ResourceType.DeploymentManager, false);
        String hostname = "a01apvl001.devillo.no";
        String username = "user";
        String password = "password";
        given().multiPart("alias", alias)
                .multiPart("type", ResourceType.DeploymentManager.name())
                .multiPart("scope.environmentclass", environment.getEnvClass())
                .multiPart("scope.environmentname", environment.getName())
                .multiPart("scope.domain", Domain.Devillo.getFqn())
                .multiPart("scope.application", application.getName())
                .multiPart("hostname", hostname)
                .multiPart("username", username)
                .multiPart("password", password)
                .auth().preemptive().basic("prodadmin", "prodadmin")
                .expect()
                .statusCode(HttpStatus.OK.value())
                .when()
                .put("/conf/resources");

        ResourceElement resource = checkResource(alias, ResourceType.DeploymentManager, true);
        assertEquals(hostname, index(resource).get("hostname").getValue());
    }

    @Test
    public void findingMultipleResourcesWithSameAliasAndEqualScopeValue_rendersIllegalArgument() {
        String resourceName = "myresource";
        repository.store(createCredentialResource(resourceName));
        repository.store(createCredentialResource(resourceName));
        expect()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .when()
                .get("/conf/resources?bestmatch=true&envClass=u&alias=" + resourceName)
                .asString();
    }

    @Test
    public void findingMultipleResourcesWithSameAliasAndEqualScopeValueWithBestMatchFalse_rendersListWithDuplicateResources() {
        String resourceName = "myresource2";
        repository.store(createCredentialResource(resourceName));
        repository.store(createCredentialResource(resourceName));
        List<ResourceElement> list = expect().log().all()
                .statusCode(HttpStatus.OK.value())
                .when()
                .get("/conf/resources?bestmatch=false&envClass=u&alias=" + resourceName)
                .xmlPath().getList("collection.resource", ResourceElement.class);
        assertThat(list.size(), equalTo(2));
    }

    @Test
    public void bestMatchWithoutApplicationParameter_shouldGiveBadRequest() {
        Resource resource = new Resource("randomResource", ResourceType.BaseUrl, new Scope(EnvironmentClass.u));
        resource.getProperties().put("url", "https://url.no");
        repository.store(resource);

        expect().statusCode(HttpStatus.OK.value())
                .when()
                .get("/conf/resources/bestmatch?envClass=u&domain=devillo.no&envName=myEnv&app=app&alias=randomResource&type=BaseUrl");

        expect().statusCode(HttpStatus.BAD_REQUEST.value())
                .when()
                .get("/conf/resources/bestmatch?envClass=u&domain=devillo.no&envName=myEnv&alias=randomResource&type=BaseUrl");
    }

    @Test
    public void whenUsageParameterIsSet_displayUsageInApplicationsField() {

        String myRequest = setupUsageInApplicationEnvironment("env1", "appName1", "resourceName1", "clusterName1", true);

        expect().statusCode(HttpStatus.OK.value())
                .body(hasXPath("/collection/resource/usedInApplications/usedInApplication/name"))
                .body(hasXPath("/collection/resource/usedInApplications/usedInApplication/envName"))
                .when()
                .get(myRequest);

    }

    @Test
    public void whenUsageParameterIsNotSet_doNotDisplayUsageInApplicationsField() {

        String myRequest = setupUsageInApplicationEnvironment("env2", "appName2", "resourceName2", "clusterName2", false);

        expect().statusCode(HttpStatus.OK.value())
                .body(not(hasXPath("/collection/resource/usedInApplications")))
                .when()
                .get(myRequest);

    }

    private String setupUsageInApplicationEnvironment(String envName, String applicationName, String resourceName, String clusterName, boolean showUsageInApplications) {

        Environment myEnvironment = repository.store(new Environment(envName, EnvironmentClass.u));

        Node myNode = new Node("host.devillo.no", "username", "password", EnvironmentClass.u, PlatformType.JBOSS);
        myEnvironment.addNode(myNode);

        Cluster myCluster = new Cluster(clusterName, Domain.Devillo);

        Application myApplication = repository.store(new Application(applicationName, "a.b.c", "c.d.e"));

        myEnvironment.addCluster(myCluster);

        myCluster.addApplication(myApplication);
        myCluster.addNode(myNode);

        repository.store(myEnvironment);

        String requestAppend = "";
        if (showUsageInApplications) {
            requestAppend = "&usage=true";
        }
        String myRequest = "/conf/resources?bestmatch=true&envClass=t&envName=" + envName + "&type=DataSource&alias=" + resourceName + requestAppend;

        Scope scope = new Scope(EnvironmentClass.t).domain(Domain.TestLocal).envName(myEnvironment.getName());

        Resource myResource = new Resource(resourceName, ResourceType.DataSource, scope);
        myResource.putPropertyAndValidate("url", "jdbc:url2");
        myResource.putSecretAndValidate("password", "secret");
        myResource.putPropertyAndValidate("username", "user");
        myResource.putPropertyAndValidate("oemEndpoint", "test");
        myResource.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        myResource = repository.store(myResource);

        ResourceReference resourceReference = new ResourceReference(myResource, 0L);

        ApplicationInstance appInstance = myCluster.getApplicationInstances().iterator().next();
        Set<ResourceReference> resourceReferenceSet = appInstance.getResourceReferences();

        resourceReferenceSet.add(resourceReference);

        repository.store(myEnvironment);

        return myRequest;
    }

    private Resource createCredentialResource(String resourceName) {
        Resource resource = new Resource(resourceName, ResourceType.Credential, new Scope(EnvironmentClass.u));
        resource.putPropertyAndValidate("username", "tull");
        resource.putSecretAndValidate("password", "ball");
        return resource;
    }

    private ImmutableMap<String, PropertyElement> index(ResourceElement resource) {
        return Maps.uniqueIndex(resource.getProperties(), new SerializableFunction<PropertyElement, String>() {
            public String process(PropertyElement input) {
                return input.getName();
            }
        });
    }
}
