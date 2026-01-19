package no.nav.aura.envconfig.spring;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.aura.envconfig.ApplicationRole;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

@SuppressWarnings("serial")
public final class User implements Serializable {

    private final String identity;
    private final Set<String> roles;
    private final boolean authenticated;
    private final String displayName;
    private final Set<String> groups;

    private User(String identity, String displayName, Collection<String> roles, Collection<String> groups, boolean authenticated) {
        this.identity = identity;
        this.displayName = displayName;
        this.roles = Set.copyOf(roles);
        this.groups = Set.copyOf(groups);
        this.authenticated = authenticated;
    }

    private User(String identity) {
        this(identity, identity, new HashSet<String>(), new HashSet<String>(), false);
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return new User("unauthenticated");
        }
        String identification = authentication.getName();
        if ("anonymousUser".equals(identification)) {
            return new User(identification);
        } else {
            User user = new User(identification, getDisplayName(authentication), getRoles(authentication), getGroups(authentication), authentication.isAuthenticated());
            return user;
        }
    }

    private static String getDisplayName(Authentication authentication) {

        if (authentication.getPrincipal() instanceof LdapUserDetails) {
            return ((LdapUserDetails) authentication.getPrincipal()).getDn();
        } else {
            return authentication.getName();
        }
    }

    public boolean isMemberOfGroup(Collection<String> expectedGroups) {
        if (!isAuthenticated()) {
            return false;
        }
        for (String expected : expectedGroups) {
            if (groups.contains(expected.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(ApplicationRole... expectedRoles) {
        for (ApplicationRole applicationRole : expectedRoles) {
            if (roles.contains(applicationRole.name().toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getIdentity() {
        return identity;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getGroups() {
        return groups;
    }

    private static Set<String> getRoles(Authentication auth) {
        return extractStringSetFromGrantedAuthority(auth.getAuthorities());
    }

    private static Set<String> getGroups(Authentication auth) {
        if (auth.getPrincipal() instanceof LdapUserDetails) {
            return extractStringSetFromGrantedAuthority(((LdapUserDetails) auth.getPrincipal()).getAuthorities());
        } else {
            return Set.of();
        }
    }

    private static Set<String> extractStringSetFromGrantedAuthority(Collection<? extends GrantedAuthority> grantedAuthorities) {
    	if (grantedAuthorities == null || grantedAuthorities.isEmpty()) {
			return Set.of();
		}
		
		return grantedAuthorities.stream()
				.map(GrantedAuthority::getAuthority)
				.map(String::toUpperCase)
				.collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).append("displayname", this.displayName).append("id", this.identity).append("roles", roles).append("groups", groups).build();
    }

}
