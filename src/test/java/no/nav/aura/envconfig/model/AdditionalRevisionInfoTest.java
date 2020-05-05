package no.nav.aura.envconfig.model;

import no.nav.aura.envconfig.auditing.NavUser;
import no.nav.aura.envconfig.model.infrastructure.Node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AdditionalRevisionInfoTest {

    @Test
    public void onbehalfWithNameandId() {
        AdditionalRevisionInfo<Node> revinfo = new AdditionalRevisionInfo<Node>();
        revinfo.setOnBehalfOf(new NavUser("id", "name"));
        assertNotNull(revinfo.getOnBehalfOf());
        assertEquals("id", revinfo.getOnBehalfOf().getId());
        assertEquals("name", revinfo.getOnBehalfOf().getName());
    }

    @Test
    public void onbehalfWithOnlyId() {
        AdditionalRevisionInfo<Node> revinfo = new AdditionalRevisionInfo<Node>();
        revinfo.setOnBehalfOf(new NavUser("id"));
        assertNotNull(revinfo.getOnBehalfOf());
        assertEquals("id", revinfo.getOnBehalfOf().getId());
        assertNull(revinfo.getOnBehalfOf().getName(), "name");
    }

    @Test
    public void onbehalfNull() {
        AdditionalRevisionInfo<Node> revinfo = new AdditionalRevisionInfo<Node>();
        assertNull(revinfo.getOnBehalfOf());
    }
}
