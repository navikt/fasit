package no.nav.aura.envconfig.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import no.nav.aura.envconfig.ApplicationRole;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.GrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthoritiesMapperTest {

    private static Collection<GrantedAuthority> authorities = Arrays.asList(
            new MockAuthority("0000-GA-ENV-CONFIG_TEST_ADMIN"),
            new MockAuthority("0000-GA-ENV-CONFIG_PROD_ADMIN"),
            new MockAuthority("0000-GA-WAS_ADM"),
            new MockAuthority("0000-GA-WAS_MON")
    );

    private static Collection<? extends GrantedAuthority> applicationRoles;

    @SuppressWarnings("serial")
    private static class MockAuthority implements GrantedAuthority {
        private String name;

        public MockAuthority(String name) {
            this.name = name;
        }

        @Override
        public String getAuthority() {
            return name;
        }
    }

    @Test
    public void testRoleMappingOperations() {
        AuthoritiesMapper mapper = new AuthoritiesMapper(new MockEnvironment()
                .withProperty("ROLE_OPERATIONS.groups", "0000-GA-ENV-CONFIG_TEST_ADMIN"
        ));
        applicationRoles = mapper.mapAuthorities(authorities);
        assertTrue(applicationRoles.size() == 2, "Incorrect role count");
        assertTrue(applicationRoles.containsAll(Set.of(ApplicationRole.ROLE_OPERATIONS, ApplicationRole.ROLE_USER)), "Missing expected roles");
    }

    @Test
    public void testRoleOperationsAndProdAdmin() {
        AuthoritiesMapper mapper = new AuthoritiesMapper(new MockEnvironment()
                .withProperty("ROLE_OPERATIONS.groups", "0000-GA-ENV-CONFIG_PROD_ADMIN")
                .withProperty("ROLE_PROD_OPERATIONS.groups", "0000-GA-ENV-CONFIG_PROD_ADMIN"));

        applicationRoles = mapper.mapAuthorities(authorities);
        assertTrue(applicationRoles.size() == 3, "Incorrect role count");
        assertTrue(applicationRoles.containsAll(Set.of(ApplicationRole.ROLE_OPERATIONS, ApplicationRole.ROLE_PROD_OPERATIONS, ApplicationRole.ROLE_USER)), "Missing expected roles");
    }

    @Test
    public void testRoleUser() {
        AuthoritiesMapper mapper = new AuthoritiesMapper(new MockEnvironment()
                .withProperty("ROLE_OPERATIONS.groups", "A_GROUP_IM_NOT_A_MEMBER_OF, ANOTHER_GROUP_IM_NOT_A_MEMBER_OF"));

        applicationRoles = mapper.mapAuthorities(authorities);
        assertTrue(applicationRoles.size() == 1, "Incorrect role count");
        assertTrue(applicationRoles.containsAll(Set.of(ApplicationRole.ROLE_USER)), "Missing expected role");
    }

    @Test
    public void testNoRolesConfiguredShouldGetRoleUser() {
        AuthoritiesMapper mapper = new AuthoritiesMapper(new MockEnvironment());
        applicationRoles = mapper.mapAuthorities(authorities);
        assertTrue(applicationRoles.size() == 1, "Incorrect role count");
        assertTrue(applicationRoles.containsAll(Set.of(ApplicationRole.ROLE_USER)), "Missing expected role");
    }

    @Test
    public void testMultipleGroupsResultingInOneRole() {
        AuthoritiesMapper mapper = new AuthoritiesMapper(new MockEnvironment()
                .withProperty("ROLE_OPERATIONS.groups", "A_GROUP_IM_NOT_A_MEMBER_OF, ANOTHER_GROUP_IM_NOT_A_MEMBER_OF, 0000-GA-ENV-CONFIG_TEST_ADMIN")
                .withProperty("ROLE_PROD_OPERATIONS.groups", "A_PROD_GROUP_IM_NOT_A_MEMBER_OF, ANOTHER_PROD_GROUP_IM_NOT_A_MEMBER_OF"));

        applicationRoles = mapper.mapAuthorities(authorities);
        assertTrue(applicationRoles.size() == 2, "Incorrect role count");
        assertTrue(applicationRoles.containsAll(Set.of(ApplicationRole.ROLE_OPERATIONS, ApplicationRole.ROLE_USER)), "Missing expected role");
    }

    @Test
    public void testSameGroupInTwoRoles() {
        AuthoritiesMapper mapper = new AuthoritiesMapper(new MockEnvironment()
                .withProperty("ROLE_OPERATIONS.groups", "0000-GA-ENV-CONFIG_TEST_ADMIN")
                .withProperty("ROLE_PROD_OPERATIONS.groups", "0000-GA-ENV-CONFIG_TEST_ADMIN"));

        applicationRoles = mapper.mapAuthorities(authorities);
        assertTrue(applicationRoles.size() == 3, "Incorrect role count");
        assertTrue(applicationRoles.containsAll(Set.of(ApplicationRole.ROLE_OPERATIONS, ApplicationRole.ROLE_PROD_OPERATIONS, ApplicationRole.ROLE_USER)), "Missing expected roles");
    }
}
