package no.nav.aura.envconfig.spring;

import static no.nav.aura.envconfig.ApplicationRole.ROLE_CI;
import static no.nav.aura.envconfig.ApplicationRole.ROLE_OPERATIONS;
import static no.nav.aura.envconfig.ApplicationRole.ROLE_PROD_OPERATIONS;
import static no.nav.aura.envconfig.ApplicationRole.ROLE_SELFSERVICE;
import static no.nav.aura.envconfig.ApplicationRole.ROLE_SELFSERVICE_PROD;
import static no.nav.aura.envconfig.ApplicationRole.ROLE_USER;

import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

public abstract class Roles {

    public static boolean hasRestrictedAccess(AccessControlled accessControlled) {
        if (SecurityByPass.isByPassEnabled()) {
            return true;
        }
        AccessControl accessControl = accessControlled.getAccessControl();
        User user = User.getCurrentUser();

        EnvironmentClass environmentClass = accessControl.getEnvironmentClass();
        return hasAccessToEnvironmentClass(environmentClass) || user.isMemberOfGroup(accessControl.getAdGroupsAsList());
    }

    private static boolean hasAccessToEnvironmentClass(EnvironmentClass environmentClass) {
        User user = User.getCurrentUser();
        if (SecurityByPass.isByPassEnabled()) {
            return true;
        }
        switch (environmentClass) {
        case p:
            return user.hasRole(ROLE_PROD_OPERATIONS, ROLE_SELFSERVICE_PROD);
        case q:
        case t:
            return user.hasRole(ROLE_PROD_OPERATIONS, ROLE_SELFSERVICE_PROD, ROLE_OPERATIONS,
                    ROLE_SELFSERVICE);
        case u:
            return user.hasRole(ROLE_USER, ROLE_CI, ROLE_PROD_OPERATIONS, ROLE_SELFSERVICE_PROD,
                    ROLE_OPERATIONS, ROLE_SELFSERVICE);
        default:
            throw new RuntimeException("Unknown environment class " + environmentClass);
        }
    }

}
