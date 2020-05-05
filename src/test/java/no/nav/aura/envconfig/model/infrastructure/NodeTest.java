package no.nav.aura.envconfig.model.infrastructure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class NodeTest {

static {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
    }


    @Test
    public void testDomainFromHostname() {
        Node node = new Node("myhost.devillo.no", "user", "password");
        assertEquals(Domain.Devillo, node.getDomain());
    }

    @Test
    public void testDomainFromHostnameWrongFormat() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Node node = new Node("illegal", "user", "password");
            node.getDomain();
        });
    }

    @Test
    public void testDomainFromHostnameUnKnownDoamin() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Node node = new Node("myhost.hotels.com", "user", "password");
            node.getDomain();
        });
    }

}
