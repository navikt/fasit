package no.nav.aura.envconfig.model;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.spring.SpringTest;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelEntityStateUpdaterTest extends SpringTest {

    private static final String PRODADMIN = "prodadmin";
    private static final String USER = "user";

    @Test
    public void storeCreatedFields() throws Exception {
        Application application = store(new Application("Test"), PRODADMIN);
        Thread.sleep(5);
        assertEquals("prodadmin (prodadmin)", application.getUpdatedBy());
        DateTime oneMinuteAgo = DateTime.now().minusMinutes(1);
        assertTrue(application.getCreated().isAfter(oneMinuteAgo));
        assertTrue(application.getCreated().isBeforeNow());
        assertEquals(application.getCreated(), application.getUpdated());
    }

    private <E extends ModelEntity> E store(E entity, String user) {
        return storeAs(user, user, entity);
    }

    @Test
    public void storeUpdatedFields() throws Exception {
        Application application = store(new Application("Test"), "operation");
        Application original = new Application(application);
        application.setName("Ball");
        assertEquals("operation (operation)", application.getUpdatedBy());
        Thread.sleep(1);
        application = store(application, PRODADMIN);
        Thread.sleep(3);

        assertEquals("prodadmin (prodadmin)", application.getUpdatedBy());
        assertEquals(original.getCreated(), application.getCreated());
        assertTrue(application.getUpdated().isAfter(application.getCreated()));
        assertTrue(application.getUpdated().isBefore(DateTime.now()));
    }
}
