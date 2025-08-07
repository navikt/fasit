package no.nav.aura.fasit.rest.security;

import javax.ws.rs.ForbiddenException;

import no.nav.aura.envconfig.ApplicationRole;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.spring.User;

public class AccessChecker {

    public static void checkSuperuserAccess() {
        if(!User.getCurrentUser().hasRole(ApplicationRole.ROLE_SUPERUSER)) {
            throw new ForbiddenException("User " + User.getCurrentUser().getIdentity() + " do not have required superuser access");
        }
    }

    public static void checkAccess(AccessControlled accessControlled){
        if (!RestRoles.hasEditAccess(accessControlled)){
            throw new ForbiddenException("User " + User.getCurrentUser().getIdentity() + " do not have access") ;
        }
    }

}
