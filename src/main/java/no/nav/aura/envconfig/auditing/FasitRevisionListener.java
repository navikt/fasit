package no.nav.aura.envconfig.auditing;

import no.nav.aura.envconfig.model.AdditionalRevisionInfo;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.spring.User;
import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class FasitRevisionListener implements EntityTrackingRevisionListener {

    private static final Logger log = LoggerFactory.getLogger(FasitRevisionListener.class);

    @Override
    public void newRevision(Object revisionEntity) {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void entityChanged(Class entityClass, String entityName, Serializable entityId, RevisionType revisionType, Object revisionEntity) {
        if (!ModelEntity.class.isAssignableFrom(entityClass)) {
            log.debug("Skipping entity {}", entityName);
            return;
        }

        AdditionalRevisionInfo<?> additionalRevisionInfo = (AdditionalRevisionInfo<?>) revisionEntity;
        additionalRevisionInfo.setModifiedEntityType(entityClass);
        additionalRevisionInfo.setAuthor(User.getCurrentUser().getDisplayName());
        additionalRevisionInfo.setAuthorId(User.getCurrentUser().getIdentity());

        additionalRevisionInfo.setMessage(EntityCommenter.getComment());
        NavUser onBehalfOfUser = EntityCommenter.getOnBehalfOfUser();
        if (onBehalfOfUser != null) {
            additionalRevisionInfo.setOnBehalfOf(onBehalfOfUser);
        }
        log.debug("Audited entity of type {} was changed. {}", entityName, additionalRevisionInfo.toString());
    }
}
