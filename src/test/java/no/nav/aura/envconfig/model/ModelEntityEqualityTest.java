package no.nav.aura.envconfig.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

public class ModelEntityEqualityTest {

    private Environment environment = new Environment("name", EnvironmentClass.u);

    @Test
    public void testSelfEquality() {
        assertTrue(environment.equals(environment));
    }

    @Test
    public void testEqualityWithNull() {
        assertFalse(environment.equals(null));
    }

}