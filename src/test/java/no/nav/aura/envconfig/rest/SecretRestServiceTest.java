package no.nav.aura.envconfig.rest;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import no.nav.aura.envconfig.ApplicationRole;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.model.secrets.Secret;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response.Status;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static no.nav.aura.envconfig.ApplicationRole.*;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecretRestServiceTest extends no.nav.aura.envconfig.rest.RestTest {

	@Test
	public void testGetSecretOnNode() {
		Secret secret = createNodeSecret(EnvironmentClass.u);

		given().auth().basic("prodadmin", "prodadmin").expect().statusCode(Status.OK.getStatusCode()).body(Matchers.equalTo("passu"))
				.when().get(SecretRestService.createPath(secret));
	}

	private Secret createNodeSecret(final EnvironmentClass environmentClass) {
		Environment environment = new Environment(environmentClass + "envfornode" + UUID.randomUUID(), environmentClass);
		environment.addNode(new Node("host" + UUID.randomUUID(), "user", "pass" + environmentClass, environmentClass, PlatformType.JBOSS));
		environment = repository.store(environment);
		Secret secret = getSingle(environment.getNodes()).getPassword();
		return secret;
	}

	private <T> T getSingle(Collection<T> ts) {
		assertEquals(1, ts.size());
		return ts.iterator().next();
	}

	@Test
	public void testGetSecretOnResource() {
		Secret secret = createResourceSecret(EnvironmentClass.u);

		given().auth().basic("prodadmin", "prodadmin").expect().statusCode(Status.OK.getStatusCode()).body(Matchers.equalTo("passu"))
				.when().get(SecretRestService.createPath(secret));
	}

	@Test
	public void testGetSecretOnResourceNotAllowed() {
		Secret secret = createResourceSecret(EnvironmentClass.t);
		given().auth().basic("user", "user").expect().statusCode(Status.UNAUTHORIZED.getStatusCode()).when()
				.get(SecretRestService.createPath(secret));
	}

	private Secret createResourceSecret(EnvironmentClass environmentClass) {
		Resource resource = new Resource("db", ResourceType.DataSource, new Scope(environmentClass));
		resource.putPropertyAndValidate("url", "https://url.com");
		resource.putPropertyAndValidate("username", "trond");
        resource.putPropertyAndValidate("oemEndpoint", "test");
		resource.putPropertyAndValidate("onsHosts", "test:6200,test1:6200");
		resource.putSecretAndValidate("password", "pass" + environmentClass);
		resource = repository.store(resource);
		Secret secret = getSingle(resource.getSecrets().values());
		return secret;
	}

	@Test
	public void testGetSecret_wrongIdFormat() {
		assert404Error("/conf/secrets/54235423", "Unable to find secret");
		assert404Error("/conf/secrets/kremfjes-47328", "Unable to find secret");
		assert404Error("/conf/secrets/secret-a473843", "Unable to find secret");
		assert404Error("/conf/secrets/resourceproperties-473843", "Unable to find secret");
		assert404Error("/conf/secrets/resourceproperties-" + createResource().getID() + "-dill", "Unable to find secret");
	}

	@Test
	public void testGetLoginSecret_missingId() {
		assert404Error("/conf/secrets/secret-4711", "Unable to find");
	}
	
	@Test
	public void checkAccess(){
		assertAccess(ROLE_ANONYMOUS, u, UNAUTHORIZED);
		assertAccess(ROLE_ANONYMOUS, t, UNAUTHORIZED);
		assertAccess(ROLE_ANONYMOUS, q, UNAUTHORIZED);
		assertAccess(ROLE_ANONYMOUS, p, UNAUTHORIZED);

		assertAccess(ROLE_USER, u, OK);
		assertAccess(ROLE_USER, t, UNAUTHORIZED);
		assertAccess(ROLE_USER, q, UNAUTHORIZED);
		assertAccess(ROLE_USER, p, UNAUTHORIZED);
		
		assertAccess(ROLE_OPERATIONS, u, OK);
		assertAccess(ROLE_OPERATIONS, t, OK);
		assertAccess(ROLE_OPERATIONS, q, OK);
		assertAccess(ROLE_OPERATIONS, p, UNAUTHORIZED);
		
		assertAccess(ROLE_PROD_OPERATIONS, u, OK);
		assertAccess(ROLE_PROD_OPERATIONS, t, OK);
		assertAccess(ROLE_PROD_OPERATIONS, q, OK);
		assertAccess(ROLE_PROD_OPERATIONS, p, OK);
		

		
	}

	private void assertAccess(ApplicationRole role, EnvironmentClass envClass, Status expectedStatus) {
		Secret secret = createNodeSecret(envClass);
		Response response = auth(role).expect().when().get(SecretRestService.createPath(secret));
		int statusCode = response.getStatusCode();
		assertEquals(expectedStatus.getStatusCode(), response.getStatusCode(), "statusCode for role " + role  + " in environment "+ envClass);
		if (statusCode == OK.getStatusCode()) {
			assertEquals("pass" + envClass, response.getBody().asString());
		}
	}



	private RequestSpecification auth(ApplicationRole role) {
		Map<ApplicationRole, ImmutablePair<String, String>> roles = new HashMap<>();
		roles.put(ApplicationRole.ROLE_OPERATIONS, new ImmutablePair<>("operation", "operation"));
		roles.put(ApplicationRole.ROLE_PROD_OPERATIONS, new ImmutablePair<>("prodadmin", "prodadmin"));
		roles.put(ApplicationRole.ROLE_USER, new ImmutablePair<>("user", "user"));
		ImmutablePair<String, String> pair = roles.get(role);
		if (pair == null) {
			return given();
		}
		return given().auth().basic(pair.left, pair.right);
	}

	
	private Resource createResource() {
		Resource resource = new Resource("what", ResourceType.Credential, new Scope(EnvironmentClass.u));
		resource.putPropertyAndValidate("username", "maximus");
		resource.putSecretAndValidate("password", "mittpass");
		resource = repository.store(resource);
		return resource;
	}

	private void assert404Error(String uri, String contains) {
		given().auth().basic("admin", "admin").expect().statusCode(Status.NOT_FOUND.getStatusCode())
				.body(Matchers.containsString(contains)).when().get(uri);
	}

}
