package no.nav.aura.envconfig.rest;

import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.path.xml.XmlPath;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.client.DeployedApplicationDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.util.Effect;
import no.nav.aura.fasit.client.model.RegisterApplicationInstancePayload;
import no.nav.aura.fasit.client.model.UsedResource;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;
import java.io.IOException;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.xml.XmlPath.from;
import static javax.ws.rs.core.Response.Status.OK;
import static no.nav.aura.envconfig.model.resource.ResourceType.BaseUrl;
import static no.nav.aura.envconfig.rest.JaxbHelper.marshal;
import static no.nav.aura.envconfig.util.TestHelper.assertAndGetSingle;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("serial")
public class ApplicationInstanceUrlTest extends RestTest {
    private static Application app;
    private static Environment env;

    @BeforeAll
    public static void setup() throws Exception {
        env = new Environment("test", EnvironmentClass.u);
        Cluster cluster = new Cluster("myCluster", Domain.Devillo);
        cluster.setLoadBalancerUrl("https://mylb.adeo.no");
        Cluster wasCluster = new Cluster("myWasCluster", Domain.Devillo);
        wasCluster.setLoadBalancerUrl("https://mylb.adeo.no");
        env.addCluster(cluster);
        env.addCluster(wasCluster);
        app = repository.store(new Application("app", "app", "no.nav.app"));
        Application newApp = repository.store(new Application("newapp", "newapp", "no.nav.newapp"));
        Application wasApp = repository.store(new Application("wasapp", "wasapp", "no.nav.was"));

        cluster.addApplication(app);
        cluster.addApplication(newApp);
        wasCluster.addApplication(wasApp);

        Node node = new Node("hostname.devillo.no", "username", "password");
        Node wasNode = new Node("washost1.devillo.no", "username", "password", EnvironmentClass.u, PlatformType.WAS);
        env.addNode(cluster, node);
        env.addNode(wasCluster, wasNode);
        env = repository.store(env);
    }

    @Test
    public void applicationInstanceGetServices_shouldBeCaseInsensitive() throws Exception {
        expect().statusCode(OK.getStatusCode()).body(containsString("wasapp")).when().get("/conf/environments/test/applications");
        expect().statusCode(OK.getStatusCode()).body(containsString("wasapp")).when().get("/conf/environments/tEsT/applications");
        expect().statusCode(OK.getStatusCode()).when().get("/conf/environments/test/applications/app");
        expect().statusCode(OK.getStatusCode()).when().get("/conf/environments/test/applications/APP");
    }

    @Test
    public void legacyGetClusters() {
        String xml = expect().defaultParser(Parser.XML).statusCode(OK.getStatusCode())
                .when().get("/conf/environments/test/applications/app/clusters").asString();
        XmlPath path = from(xml);
        assertEquals(1, path.getInt("collection.cluster.size()"), "clusters");
        assertEquals("myCluster", path.getString("collection.cluster.name"), "cluster name");
        assertEquals("devillo.no", path.getString("collection.cluster.domain"), "cluster domain");
        assertEquals(1, path.getInt("collection.cluster.nodes.size()"), "nodes");
        assertEquals("hostname.devillo.no", path.getString("collection.cluster.nodes[0].hostname"));
        assertEquals("username", path.getString("collection.cluster.nodes[0].username"));
        assertEquals("WILDFLY", path.getString("collection.cluster.nodes[0].platformType"));
    }

    @Test
    public void getApplicationInstance() {
        String xml = expect().defaultParser(Parser.XML).statusCode(OK.getStatusCode())
                .when().get("/conf/environments/test/applications/app").asString();
        XmlPath path = from(xml);
        assertEquals("app", path.getString("application.name"), "app name");
        assertEquals("myCluster", path.getString("application.cluster.name"), "cluster name");
        assertEquals("devillo.no", path.getString("application.cluster.domain"), "cluster domain");
        assertEquals(2, path.getInt("application.cluster.application.size()"), "cluster applications");
        assertThat("cluster applications app", xml, containsString("<application>app</application>"));
        assertThat("cluster applications newapp", xml, containsString("<application>newapp</application>"));
        assertEquals(1, path.getInt("application.cluster.nodes.size()"), "nodes");
        assertEquals("hostname.devillo.no", path.getString("application.cluster.nodes[0].hostname"));
        assertEquals("username", path.getString("application.cluster.nodes[0].username"));
        assertEquals("WILDFLY", path.getString("application.cluster.nodes[0].platformType"));
    }

    @Test
    public void getWasApplication() {
        String xml = expect().defaultParser(Parser.XML).statusCode(OK.getStatusCode())
                .when().get("/conf/environments/test/applications/wasapp").asString();
        XmlPath path = from(xml);
        assertEquals("myWasCluster", path.getString("application.cluster.name"), "cluster name");
        assertEquals(1, path.getInt("application.cluster.nodes.size()"), "nodes");
        assertEquals("WAS", path.getString("application.cluster.nodes[0].platformType"));
    }

    @Test
    public void envNotFound() {
        expect().statusCode(Status.NOT_FOUND.getStatusCode())
                .when().get("/conf/environments/unknownName/applications/app");
    }

    @Test
    public void appNotFound() {
        expect().statusCode(Status.NOT_FOUND.getStatusCode())
                .when().get("/conf/environments/test/applications/unknown");
    }

    @Test
    public void registerApplicationInstanceCheckEntityComment() throws Exception {
        byte[] deployedApplicationBS = createDeployedApplicationDO("app-config.xml", "1.0.0");

        given().auth().preemptive().basic("prodadmin", "prodadmin")
                .request().body(deployedApplicationBS)
                .queryParam("entityStoreComment", "Application app deployed to envionment test")
                .header("x-onbehalfof", "otheruser")
                .contentType(ContentType.XML)
                .expect().body(equalTo("")).statusCode(Status.NO_CONTENT.getStatusCode())
                .when().put("/conf/environments/test/applications/app");

        testBean.perform(new Effect() {
            public void perform() {
                final Application application = repository.findApplicationByName("app");
                ApplicationInstance applicationInstance = assertAndGetSingle(repository.findApplicationInstancesBy(application));
                assertEquals("1.0.0", applicationInstance.getVersion());

                FasitRevision<ApplicationInstance> revision = getHeadrevision(applicationInstance);
                assertEquals("prodadmin", revision.getAuthor());
                assertEquals("otheruser", revision.getOnbehalfOf().getId());
                assertThat(revision.getMessage(), startsWith("Application app"));

            }
        });
    }

    @Test
    public void registerApplicationV1Simple() throws Exception {

        byte[] payload = getAppConfig("/payloads/registerapplicationinstance-min.json");
        given().auth().preemptive().basic("prodadmin", "prodadmin")
                .request().body(payload)
                .queryParam("entityStoreComment", "Application app deployed to envionment test")
                .header("x-onbehalfof", "otheruser")
                .contentType(ContentType.JSON)
                .expect().statusCode(Status.CREATED.getStatusCode())

                .when().post("/conf/v1/applicationinstances");

        testBean.perform(new Effect() {
            public void perform() {
                final Application application = repository.findApplicationByName("app");
                ApplicationInstance applicationInstance = assertAndGetSingle(repository.findApplicationInstancesBy(application));
                assertEquals("1.0.0", applicationInstance.getVersion());

                FasitRevision<ApplicationInstance> revision = getHeadrevision(applicationInstance);
                assertEquals("prodadmin", revision.getAuthor());
                assertEquals("otheruser", revision.getOnbehalfOf().getId());
                assertThat(revision.getMessage(), startsWith("Application app"));

            }
        });
    }

    @Test
    public void verifyApplicationNotAuthenticated() throws Exception {
        given()
                .request().body(getAppConfig("app-config.xml"))
                .contentType(ContentType.XML)
                .expect().statusCode(Status.NO_CONTENT.getStatusCode())
                .when().put("/conf/environments/test/applications/app/verify");
    }

    @Test
    public void verifyApplicationAuthenticated() throws Exception {
        given().auth().preemptive().basic("prodadmin", "prodadmin")
                .request().body(getAppConfig("app-config.xml"))
                .contentType(ContentType.XML)
                .expect().statusCode(Status.NO_CONTENT.getStatusCode())
                .when().put("/conf/environments/test/applications/app/verify");
    }

    @Test
    public void undeployApplicationInstance() {
        final Application application = app;

        testBean.perform(new Effect() {
            public void perform() {
                ApplicationInstance appInstance = assertAndGetSingle(repository.findApplicationInstancesBy(application));
                appInstance.setVersion("1.0");
                repository.store(appInstance);
            }
        });

        given().auth().preemptive().basic("prodadmin", "prodadmin")
                .queryParam("entityStoreComment", "undeployed app")
                .expect().body(equalTo("")).statusCode(Status.NO_CONTENT.getStatusCode())
                .when().delete("/conf/environments/test/applications/app");

        testBean.perform(new Effect() {
            public void perform() {
                ApplicationInstance applicationInstance = assertAndGetSingle(repository.findApplicationInstancesBy(application));
                assertNull(applicationInstance.getVersion(), "Version");
                assertEquals(getHeadrevision(applicationInstance).getMessage(), "undeployed app");
            }

        });
    }

    @Test
    public void resourceReferencesContainsHeadRevision() throws Exception {
        env = new Environment("env", EnvironmentClass.u);
        Cluster cluster = new Cluster("cluster", Domain.Devillo);
        cluster.setLoadBalancerUrl("https://mylb.adeo.no");
        env.addCluster(cluster);
        app = repository.store(new Application("application", "dapp", "no.nav.smapp"));

        cluster.addApplication(app);

        Node node = new Node("host2.devillo.no", "username", "password");
        env.addNode(cluster, node);
        repository.store(env);

        Resource resource1 = new Resource("aBaseUrl", BaseUrl, new Scope(EnvironmentClass.u));
        resource1.getProperties().put("url", "http://url.com");
        resource1 = repository.store(resource1);

        Resource resource2 = new Resource("anotherBaseUrl", BaseUrl, new Scope(EnvironmentClass.u));
        resource2.getProperties().put("url", "http://url.com");
        resource2 = repository.store(resource2);
        resource2.getProperties().put("url", "http://url2.com");
        resource2 = repository.store(resource2);

        RegisterApplicationInstancePayload payload = new RegisterApplicationInstancePayload(app.getName(), "1.0.0", env.getName());
        payload.getNodes().add(node.getHostname());
        payload.addUsedResources(new UsedResource(resource1.getID(), getHeadrevision(resource1).getRevision()));
        payload.addUsedResources(new UsedResource(resource2.getID(), getHeadrevision(resource2).getRevision()));

        given().auth().preemptive().basic("prodadmin", "prodadmin")
                .request().body(payload.toJson())
                .contentType(ContentType.JSON)
                .expect().statusCode(Status.CREATED.getStatusCode())
                .when().post("/conf/v1/applicationinstances");

        testBean.perform(new Effect() {
            public void perform() {
                ApplicationInstance applicationInstance = repository.findApplicationInstancesBy(app).iterator().next();
                for (ResourceReference resourceReference : applicationInstance.getResourceReferences()) {
                    FasitRevision<Resource> headrevision = getHeadrevision(Resource.class, resourceReference.getResource().getID());
                    assertThat(resourceReference.getRevision(), is(headrevision.getRevision()));
                }
            }
        });

    }

    private byte[] getAppConfig(String appConfigFileName) throws IOException {
        return IOUtils.toByteArray(getClass().getResourceAsStream(appConfigFileName));
    }

    private byte[] createDeployedApplicationDO(String appconfigFileName, String version) throws JAXBException {
        no.nav.aura.appconfig.Application appConfig = no.nav.aura.appconfig.Application.instance(getClass().getResourceAsStream(appconfigFileName));
        return marshal(new DeployedApplicationDO(appConfig, version));
    }
}
