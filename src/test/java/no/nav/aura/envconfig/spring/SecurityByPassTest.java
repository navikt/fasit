package no.nav.aura.envconfig.spring;

import no.nav.aura.envconfig.util.Effect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
