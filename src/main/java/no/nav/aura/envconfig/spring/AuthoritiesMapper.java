package no.nav.aura.envconfig.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import no.nav.aura.envconfig.ApplicationRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

public class AuthoritiesMapper implements GrantedAuthoritiesMapper {

    private static final Logger log = LoggerFactory.getLogger(AuthoritiesMapper.class);
    private Map<String, Set<ApplicationRole>> groupRoleMap;
    private final Environment environment;

    public AuthoritiesMapper(Environment environment) {
        this.environment = environment;
        groupRoleMap = new HashMap<>();
        initGroupRoleMapping();
    }

    private void initGroupRoleMapping() {
        Set<ApplicationRole> applicationRoles = EnumSet.allOf(ApplicationRole.class);

        for (ApplicationRole applicationRole : applicationRoles) {
            // Get a comma separated list of LDAP group names that have the current role from the runtime container
            String groupString = environment.getProperty(applicationRole.name() + ".groups");
            if (groupString != null) {
                log.info(String.format("Application role %s is mapped to the following LDAP groups %s", applicationRole.name(), groupString));
                addGroupRoleMapping(groupString, applicationRole);
            }
        }
    }

    /**
     * Creates a reverse Map on the format ldapGroupName=ApplicationRole1,ApplicationRole2 etc This makes it faster and easier
     * to compare with the group names (authorities) from LDAP passed in to the mapAuthorities method
     * */
    private void addGroupRoleMapping(String groupString, ApplicationRole applicationRole) {
        for (String ldapGroup : Arrays.asList(groupString.split(","))) {
            String ldapGroupName = ldapGroup.trim();
            if (groupRoleMap.get(ldapGroupName) != null) {
                groupRoleMap.get(ldapGroupName).add(applicationRole);
            } else {
                groupRoleMap.put(ldapGroupName, Set.of(applicationRole));
            }
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> roles = new HashSet<>();

        roles.add(ApplicationRole.ROLE_USER);
        for (GrantedAuthority authority : authorities) {
            if (groupRoleMap.containsKey(authority.getAuthority())) {
                roles.addAll(groupRoleMap.get(authority.getAuthority()));
            }
        }

        return roles;
    }
}
