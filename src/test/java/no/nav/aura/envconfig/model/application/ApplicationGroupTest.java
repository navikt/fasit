package no.nav.aura.envconfig.model.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ApplicationGroupTest {

    @Test
    public void testApplicationGroup() {
        ApplicationGroup appGroup = new ApplicationGroup("appGroup", new Application("myApp"));
        assertEquals("appGroup", appGroup.getName());
        assertEquals(1, appGroup.getApplications().size());
        assertEquals("myApp", appGroup.getApplications().iterator().next().getName());

    }
}
