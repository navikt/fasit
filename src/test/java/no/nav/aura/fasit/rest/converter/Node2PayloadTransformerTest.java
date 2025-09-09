package no.nav.aura.fasit.rest.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.envconfig.spring.SpringTest;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.fasit.rest.model.NodePayload;

public class Node2PayloadTransformerTest extends SpringTest {

    private Node2PayloadTransformer transformer;

    @Autowired
    private NodeRepository nodeRepository;
    
    private Environment env; 
    private Node node;

    @BeforeEach
    public void setup() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
//        NodeRepository repoMock = Mockito.mock(NodeRepository.class);
        env = repository.store(new Environment("testEnv", EnvironmentClass.u));
        node = new Node("host.devillo.no", "user", "password", EnvironmentClass.u, PlatformType.JBOSS);
        transformer = new Node2PayloadTransformer(nodeRepository, URI.create("http://mocked.no"));
    }
    
    @AfterEach
    public void tearDown() {
    	nodeRepository.delete(node);
    	repository.delete(env);
    }

    @Test
    public void transformNode() {
        node.setCreated(ZonedDateTime.parse("2016-05-10T10:34:00+02"));
        nodeRepository.save(node);
        env.addNode(node);
        repository.store(env);
        NodePayload payload = transformer.apply(node);
        assertEquals("host.devillo.no", payload.hostname);
        assertEquals("user", payload.username);
    }
}
