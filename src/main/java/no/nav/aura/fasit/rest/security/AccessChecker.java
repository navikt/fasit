package no.nav.aura.fasit.rest.security;

import no.nav.aura.envconfig.ApplicationRole;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.spring.User;

import org.springframework.security.access.AccessDeniedException;

public class AccessChecker {

    public static void checkSuperuserAccess() {
        if(!User.getCurrentUser().hasRole(ApplicationRole.ROLE_SUPERUSER)) {
            throw new AccessDeniedException("User " + User.getCurrentUser().getIdentity() + " do not have required superuser access");
        }
    }

    public static void checkAccess(AccessControlled accessControlled){
        if (!RestRoles.hasEditAccess(accessControlled)){
            throw new AccessDeniedException("User " + User.getCurrentUser().getIdentity() + " do not have access") ;
        }
    }

}
