package no.nav.aura.envconfig.model.secrets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KeyFactoryTest {

    @Test
    public void keyfactory() throws Exception {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
        KeyFactory keyFactory = new KeyFactory();
        assertNotNull(keyFactory.getKeyForEnvironmentClass(EnvironmentClass.u));
        assertNotNull(keyFactory.getKeyForEnvironmentClass(EnvironmentClass.t));
        assertNotNull(keyFactory.getKeyForEnvironmentClass(EnvironmentClass.q));
        assertNotNull(keyFactory.getKeyForEnvironmentClass(EnvironmentClass.p));
    }

    @Test
    public void wrongpassword() throws Exception {
        Assertions.assertThrows(RuntimeException.class, () -> {
            System.setProperty("fasit.encryptionkeys.username", "junit");
            System.setProperty("fasit.encryptionkeys.password", "wrong");
            new KeyFactory();
        });
    }

    @Test
    public void wrongUser() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            System.setProperty("fasit.encryptionkeys.username", "unknown");
            System.setProperty("fasit.encryptionkeys.password", "wrong");
            new KeyFactory();
        });
    }

    @Test
    public void noUser() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            System.getProperties().remove("fasit.encryptionkeys.username");
            System.getProperties().remove("fasit.encryptionkeys.password");
            new KeyFactory();
        });
    }

}
