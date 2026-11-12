package no.nav.aura.envconfig.spring;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.ModelEntity;

@Aspect
public class SecurityAccessCheckAspect {

    @Before(value = "(execution(* no.nav.aura.envconfig.FasitRepository.store(..)) || execution(* no.nav.aura.envconfig.FasitRepository.delete(..))) && args(entity)")
    public void checkUpdateAccess(ModelEntity entity) throws AccessException {
        if (entity instanceof AccessControlled accessControlled) {

            if (!Roles.hasRestrictedAccess(accessControlled)) {
                throw new AccessException("No edit access for user " + User.getCurrentUser() + " for entity " + entity.getClass().getSimpleName() + " id:" + entity.getID());
            }
        }
    }

}
