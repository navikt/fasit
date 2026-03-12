package no.nav.aura.envconfig.spring;

import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.p;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.q;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.t;
import static no.nav.aura.envconfig.model.infrastructure.EnvironmentClass.u;
import static no.nav.aura.envconfig.spring.Roles.hasRestrictedAccess;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


import no.nav.aura.envconfig.ApplicationRole;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;

public class RolesTest extends SpringTest {

    private void setupUser(String adgroup, ApplicationRole... roles) {
        Authentication auth = createAuthentication("user", "user", Set.of(roles), Set.of(adgroup));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void checkAccessForUserBasedOnRole() {
        setupUser("Gruppe", ApplicationRole.ROLE_USER);
        assertTrue(hasRestrictedAccess(resource(u, null)));
        assertFalse(hasRestrictedAccess(resource(t, null)));
        assertFalse(hasRestrictedAccess(resource(q, null)));
        assertFalse(hasRestrictedAccess(resource(p, null)));
    }

    @Test
    public void checkAccessForUserBasedOnAdGroup() {
        setupUser("Gruppe69", ApplicationRole.ROLE_USER);
        
        assertTrue(hasRestrictedAccess(resource(u, "Gruppe69")), "gruppe og rolle ok");
        assertTrue(hasRestrictedAccess(resource(u, "ukjentgruppe")), "rolle ok");
        assertTrue(hasRestrictedAccess(resource(t, "Gruppe69")), "gruppe ok");
        assertFalse(hasRestrictedAccess(resource(t, "ukjentgruppe")), "hverken gruppe eller rolle");
        assertTrue(hasRestrictedAccess(resource(q, "Gruppe69")), "gruppe ok");
        assertFalse(hasRestrictedAccess(resource(q, "ukjentgruppe")), "hverken gruppe eller rolle");
        assertTrue(hasRestrictedAccess(resource(p, "Gruppe69")), "gruppe ok");
        assertFalse(hasRestrictedAccess(resource(p, "ukjentgruppe")), "hverken gruppe eller rolle");
    }

    private Resource resource(EnvironmentClass envClass, String adGroup) {
        Resource resource = new Resource("alias", ResourceType.Credential, new Scope(envClass));
        resource.getAccessControl().setAdGroups(adGroup);
        return resource;
    }

}
