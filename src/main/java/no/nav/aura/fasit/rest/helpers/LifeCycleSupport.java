package no.nav.aura.fasit.rest.helpers;

import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.auditing.NavUser;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.spring.User;
import no.nav.aura.fasit.rest.model.EntityPayload;
import no.nav.aura.fasit.rest.model.LifecyclePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static no.nav.aura.envconfig.model.deletion.LifeCycleStatus.*;

/**
 * Convinience methods for lifecyclesupport
 */
@Component
public class LifeCycleSupport {

    private final static Logger log = LoggerFactory.getLogger(LifeCycleSupport.class);

    public void update(DeleteableEntity deleteable, EntityPayload payload) {
        LifecyclePayload statusPayload = payload.lifecycle;
        update(deleteable, statusPayload);
    }

    public void update(DeleteableEntity deleteable, LifecyclePayload payload ) {
        if (payload != null && payload.status != null) {
            LifeCycleStatus originalStatus = deleteable.getLifeCycleStatus();
            LifeCycleStatus newStatus = payload.status;
            if (newStatus.equals(originalStatus)) {
                log.debug("No status update for {}. Status is allready {}", deleteable.getName(), newStatus);
                return;
            }
            if (ALERTED.equals(newStatus)) {
                log.warn("Alerting is not implemented yet");
            }

            if (STOPPED.equals(newStatus)) {
                stop(deleteable);
            }
            if (RUNNING.equals(newStatus)) {
                start(deleteable);
            }
        }
    }

    public void delete(DeleteableEntity deleteable) {
        String comment = "User %s deleted %s:%s with comment \"%s\"".formatted(getUser(), deleteable.getClass().getSimpleName(), deleteable.getName(), EntityCommenter.getComment());
        log.info("Cleaning up status after deletion of {}:{}:{} with comment {}", deleteable.getClass().getSimpleName(), deleteable.getName(), deleteable.getID(), comment);
    }
   
   

    protected void stop(DeleteableEntity deleteable) {
        if (LifeCycleStatus.STOPPED != deleteable.getLifeCycleStatus()) {
            String comment = "User %s stopped %s:%s with comment \"%s\"".formatted(getUser(), deleteable.getClass().getSimpleName(), deleteable.getName(), EntityCommenter.getComment());
            log.info("Changing status on {} to stopped  with comment {}",deleteable.getName(), comment);
            deleteable.changeStatus(STOPPED);
        }
    }

    protected void start(DeleteableEntity deleteable) {
        if (deleteable.getLifeCycleStatus()!= null && LifeCycleStatus.RUNNING !=deleteable.getLifeCycleStatus()) {
            String comment = "User %s started %s:%s with comment \"%s\"".formatted(getUser(), deleteable.getClass().getSimpleName(), deleteable.getName(), EntityCommenter.getComment());
            log.debug("Clearing status on {} with comment {}",deleteable.getName(), comment);
            deleteable.resetStatus();
        }
    }
    
    private String getUser() {
        Optional<NavUser> onBehalfOfUser = Optional.ofNullable(EntityCommenter.getOnBehalfOfUser());
        if (onBehalfOfUser.isPresent()) {
            return User.getCurrentUser().getDisplayName() + "on behalf of " + onBehalfOfUser.get().getDisplayName();
        }
        return User.getCurrentUser().getDisplayName();
    }
}
