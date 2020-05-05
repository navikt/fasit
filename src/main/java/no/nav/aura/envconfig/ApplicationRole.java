package no.nav.aura.envconfig;

import org.springframework.security.core.GrantedAuthority;

public enum ApplicationRole implements GrantedAuthority {
    ROLE_ANONYMOUS,
    /** Non personal users used from jenkins etc. */
    ROLE_CI,
    /** All authenticated users */
    ROLE_USER,
    /** Users with self service deploy rights to t & q*/
    ROLE_SELFSERVICE,
    /** Operator for T and Q */
    ROLE_OPERATIONS,
    /** Users with self service deploy rights to prod */
    ROLE_SELFSERVICE_PROD,
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
