package no.nav.aura.envconfig.spring;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import no.nav.aura.envconfig.ApplicationRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Sets;

public class UserTest extends SpringTest {

    private User user;

    @BeforeEach
    public void setup() {
        Authentication auth = createAuthentication("user", "user", Sets.newHashSet(ApplicationRole.ROLE_USER), Sets.newHashSet("Gruppe69", "Gruppe96"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        user = User.getCurrentUser();
    }


    @Test
    public void testUserGroups() {
        assertThat(user.isAuthenticated(), is(true));
        assertTrue(user.isMemberOfGroup(Sets.newHashSet("Gruppe69")));
        assertTrue(user.isMemberOfGroup(Sets.newHashSet("Gruppe96", "Enannengruppe")));
        assertFalse(user.isMemberOfGroup(Sets.newHashSet("veldighemmeliggruppe")));
    }


    @Test
    public void testUserRoles() {
        assertThat(user.isAuthenticated(), is(true));
        assertTrue(user.hasRole(ApplicationRole.ROLE_USER));
        assertFalse(user.hasRole(ApplicationRole.ROLE_PROD_OPERATIONS));
    }

}
