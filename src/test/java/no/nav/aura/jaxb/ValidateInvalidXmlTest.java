package no.nav.aura.jaxb;


import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import no.nav.aura.appconfig.Application;

public class ValidateInvalidXmlTest {

    @Test
    public void parse() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Application.instance(getClass().getResourceAsStream("/app-config-invalid.xml")));

    }

}
