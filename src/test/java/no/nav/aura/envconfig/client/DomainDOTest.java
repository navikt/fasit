package no.nav.aura.envconfig.client;

import org.junit.jupiter.api.Test;

import static no.nav.aura.envconfig.client.DomainDO.*;
import static no.nav.aura.envconfig.client.DomainDO.Zone.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DomainDOTest {

    @Test
    public void whenDomainIsDevillo_ZoneWillBeBothSbsAnsFss() {
        DomainDO domainDO = fromFqdn("devillo.no");
        assertTrue(domainDO.isInZone(SBS));
        assertTrue(domainDO.isInZone(FSS));
    }

    @Test
    public void whenDomainIsTestLocal_ZoneIsFss() {
        DomainDO domainDO = fromFqdn("test.local");
        assertTrue(domainDO.isInZone(FSS));
        assertFalse(domainDO.isInZone(SBS));
    }
    

}
