package no.nav.aura.envconfig.model.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.secrets.Secret;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResourceTest {

    public ResourceTest() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
    }

    @Test
    public void getProperties() {
        Resource resource = new Resource("alias", ResourceType.DataSource, new Scope(EnvironmentClass.u));
        resource.putPropertyAndValidate("url", "propvalue");
        resource.putPropertyAndValidate("username", "defaultuser");
        resource.putSecretAndValidate("password", "defaultpassword");

        Map<String, String> properties = resource.getProperties();
        assertEquals("propvalue", properties.get("url"));

        Secret secret = resource.getSecrets().get("password");
        assertNotNull(secret);
        assertEquals("defaultuser", resource.getProperties().get("username"));
        assertEquals("defaultpassword", secret.getClearTextString());
    }

    @Test
    public void wrongPropertyName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Resource resource = new Resource("alias", ResourceType.DataSource, new Scope(EnvironmentClass.u));
            resource.putPropertyAndValidate("errorproperty", "propvalue");
        });
    }

    @Test
    public void wrongSecretName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Resource resource = new Resource("alias", ResourceType.DataSource, new Scope(EnvironmentClass.u));
            resource.putSecretAndValidate("errorpassword", "password");
        });
    }

    @Test
    public void defaultProperties() {
        Resource resource = new Resource("alias", ResourceType.DataSource, new Scope(EnvironmentClass.u));

        assertThat(resource.getProperties().keySet(), CoreMatchers.hasItems("url", "username"));
        assertThat(resource.getSecrets().keySet(), CoreMatchers.hasItems("password"));
    }

    @Test
    public void applicationProperties() {
        Resource resource = new Resource("alias", ResourceType.ApplicationProperties, new Scope(EnvironmentClass.u));
        resource.putPropertyAndValidate("applicationProperties", "propvalue");
    }

}
