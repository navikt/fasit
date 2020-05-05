package no.nav.aura.envconfig;

import org.springframework.security.core.GrantedAuthority;

public enum ApplicationRole implements GrantedAuthority {
    ROLE_ANONYMOUS,
    /** All authenticated users */
    ROLE_USER,
    /** Operator for T and Q */
    ROLE_OPERATIONS,
    /** Operator for P */
    ROLE_PROD_OPERATIONS,
    /** Can change accessrules */
    ROLE_SUPERUSER;

    @Override
    public String getAuthority() {
        return name();
    }

    @Override
    public String toString() {
        return name();
    }

}
