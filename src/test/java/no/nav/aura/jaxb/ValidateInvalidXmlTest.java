package no.nav.aura.jaxb;

import static org.junit.Assert.assertNotNull;
import no.nav.aura.appconfig.Application;

import org.junit.Test;

public class ValidateInvalidXmlTest {

    @Test(expected = IllegalArgumentException.class)
    public void parse() throws Exception {
        Application app = Application.instance(getClass().getResourceAsStream("/app-config-invalid.xml"));
        assertNotNull(app);
    }

}
