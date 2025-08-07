package no.nav.aura.jaxb;


import no.nav.aura.appconfig.Application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidateInvalidXmlTest {

    @Test
    public void parse() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Application.instance(getClass().getResourceAsStream("/app-config-invalid.xml")));

    }

}
