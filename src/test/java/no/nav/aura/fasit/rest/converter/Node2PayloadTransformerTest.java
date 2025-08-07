package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.fasit.rest.model.NodePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Node2PayloadTransformerTest {

    private Node2PayloadTransformer transformer;

    @BeforeEach
    public void setup() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
        NodeRepository repoMock = Mockito.mock(NodeRepository.class);
        transformer = new Node2PayloadTransformer(repoMock, URI.create("http://mocked.no"));
    }

    @Test
    public void transformNode() {
        Node node = new Node("host.devillo.no", "user", "password", EnvironmentClass.u, PlatformType.JBOSS);
        node.setCreated(ZonedDateTime.parse("2016-05-10T10:34:00+02"));
        NodePayload payload = transformer.apply(node);
        assertEquals("host.devillo.no", payload.hostname);
        assertEquals("user", payload.username);
    }
}
