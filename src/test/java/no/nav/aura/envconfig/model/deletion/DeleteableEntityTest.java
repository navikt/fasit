package no.nav.aura.envconfig.model.deletion;

import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.util.TestHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeleteableEntityTest {

    private Node node;

    @BeforeEach
    public void setUp() {
        TestHelper.setUpEncryption();
        node = new Node("node.devillo.no", "user", "password");
    }

    @Test
    public void changeStatusStopped() {
        node.changeStatus(LifeCycleStatus.STOPPED);
        assertEquals(LifeCycleStatus.STOPPED, node.getLifeCycleStatus());
    }
}
