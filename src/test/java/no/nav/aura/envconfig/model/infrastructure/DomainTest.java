package no.nav.aura.envconfig.model.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DomainTest {

    @Test
    public void getDomainsForU() {
        assertEquals(3, Domain.getByEnvironmentClass(EnvironmentClass.u).size());
    }

    @Test
    public void getAllDomainsForNullInput() {
        assertTrue(Domain.getByEnvironmentClass(null).size() > 5);
    }
}
