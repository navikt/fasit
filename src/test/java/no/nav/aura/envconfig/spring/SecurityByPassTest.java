package no.nav.aura.envconfig.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import no.nav.aura.envconfig.util.Effect;

public class SecurityByPassTest {

    @Test
    @SuppressWarnings("serial")
    public void test() {
        assertFalse(SecurityByPass.isByPassEnabled());
        SecurityByPass.byPass(new Effect() {
            public void perform() {
                assertTrue(SecurityByPass.isByPassEnabled());
                SecurityByPass.byPass(new Effect() {
                    public void perform() {
                        assertTrue(SecurityByPass.isByPassEnabled());
                    }
                });
                assertTrue(SecurityByPass.isByPassEnabled());
            }
        });
        assertFalse(SecurityByPass.isByPassEnabled());
    }

}
