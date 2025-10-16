package no.nav.aura.envconfig.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import io.restassured.http.ContentType;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;

public class NavSearchRestTest extends RestTest {
	
	
	@BeforeAll
	public void setup() {
        Environment utvEnv = repository.store(new Environment("u1", EnvironmentClass.u));
        Environment qaEnv = repository.store(new Environment("q1", EnvironmentClass.q));
        Cluster appClusteru = utvEnv.addCluster(new Cluster("testCluster", Domain.Devillo));
        Cluster appClusterq = qaEnv.addCluster(new Cluster("testCluster", Domain.PreProd));
        
        Application app = repository.store(new Application("test"));
        appClusteru.addApplication(app);
        appClusterq.addApplication(app);
        
        Node newNodeu = new Node("testNode.devillo.no", "username", "password");
        Node newNodeq = new Node("testNode2.preprod.local", "username", "password");
        utvEnv.addNode(appClusteru, newNodeu);
        qaEnv.addNode(appClusterq, newNodeq);
        
        appClusteru.addNode(newNodeu);
        appClusterq.addNode(newNodeq);
        
        repository.store(utvEnv);
        repository.store(qaEnv);
        
        Resource db = new Resource("testDb", ResourceType.DataSource, new Scope(EnvironmentClass.u).domain(Domain.Devillo).envName("u1").application(app));
        db.putPropertyAndValidate("url", "jdbc:url");
        db.putPropertyAndValidate("username", "user");
        db.putPropertyAndValidate("oemEndpoint", "test");
        db.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
        db.putSecretAndValidate("password", "secret");
        repository.store(db);

	}
	
    @AfterAll
    void tearDown() {
		cleanupApplications();
		cleanupResources();
		cleanupEnvironments();
	}
    
    @Test
    public void searchForTerm() {
		String search = "myDB";
		given()
			.when()
			.get("/api/v1/navsearch?q=test&maxCount=10")
			.then()
//			.log().all()
			.statusCode(HttpStatus.OK.value())
			.contentType(ContentType.JSON)
			.body("$", hasSize(8))
			.body("name", hasItems("testDb", "testNode.devillo.no", "testCluster", "test"));
		
			
	}
    
    @Test
    public void searchWithEnvironmentBeforeTerm() {
		String search = "u1 myDB";
		given()
			.when()
			.get("/api/v1/navsearch?q=q1%20test&maxCount=10")
			.then()
			.log().all()
			.statusCode(HttpStatus.OK.value())
			.body("$", hasSize(1));
		
	}
	
}
