package no.nav.aura.envconfig.spring;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import no.nav.aura.envconfig.ApplicationRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


public class UserTest extends SpringTest {

    private User user;

    @BeforeEach
    public void setup() {
        Authentication auth = createAuthentication("user", "user", Set.of(ApplicationRole.ROLE_USER), Set.of("Gruppe69", "Gruppe96"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        user = User.getCurrentUser();
    }


    @Test
    public void testUserGroups() {
        assertThat(user.isAuthenticated(), is(true));
        assertTrue(user.isMemberOfGroup(Set.of("Gruppe69")));
        assertTrue(user.isMemberOfGroup(Set.of("Gruppe96", "Enannengruppe")));
        assertFalse(user.isMemberOfGroup(Set.of("veldighemmeliggruppe")));
    }


    @Test
    public void testUserRoles() {
        assertThat(user.isAuthenticated(), is(true));
        assertTrue(user.hasRole(ApplicationRole.ROLE_USER));
        assertFalse(user.hasRole(ApplicationRole.ROLE_PROD_OPERATIONS));
    }

}
