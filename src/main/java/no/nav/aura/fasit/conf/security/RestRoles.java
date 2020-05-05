package no.nav.aura.fasit.conf.security;

import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.spring.Roles;

public final class RestRoles extends Roles {

    public static final boolean hasViewPasswordAccess(AccessControlled entity) {
        return hasRestrictedAccess(entity);
    }

    public static final boolean hasEditAccess(AccessControlled entity) {
        return hasViewPasswordAccess(entity);
    }

}
