package no.nav.aura.envconfig.auditing;

import no.nav.aura.envconfig.model.AdditionalRevisionInfo;
import no.nav.aura.envconfig.model.ModelEntity;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.hibernate.envers.RevisionType;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.lang.reflect.Field;

@SuppressWarnings("serial")
public class FasitRevision<T extends ModelEntity> implements Serializable {
    private final long revision;
    private final DateTime timestamp;
    private final String author;
    private final NavUser onBehalfOf;
    private final String authorId;
    private final String message;
    private final Class<T> modifiedEntityType;
    private final T modelEntity;
    private RevisionType revisionType;

    public FasitRevision(AdditionalRevisionInfo<T> revisionInfo, T modelEntity) {
        this.modelEntity = modelEntity;
        this.revision = revisionInfo.getRevision();
        this.timestamp = revisionInfo.getTimestamp();
        this.author = revisionInfo.getAuthor();
        this.authorId = revisionInfo.getAuthorId();
        this.message = revisionInfo.getMessage();
        this.modifiedEntityType = revisionInfo.getModifiedEntityType();
        this.onBehalfOf = revisionInfo.getOnBehalfOf();
    }
    
    public FasitRevision(AdditionalRevisionInfo<T> revisionInfo, T modelEntity, RevisionType revisionType) {
        this(revisionInfo, modelEntity);
        this.revisionType= revisionType;
    }

    public long getRevision() {
        return revision;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public Class<T> getModifiedEntityType() {
        return modifiedEntityType;
    }

    public T getModelEntity() {
        return modelEntity;
    }

    @Override
    public String toString() {
        return (new ReflectionToStringBuilder(this) {
            protected boolean accept(Field f) {
                return super.accept(f) && !"modelEntity".equals(f.getName());
            }
        }).toString();

    }

    public String getAuthorId() {
        return authorId;
    }

    public NavUser getOnbehalfOf() {
        return onBehalfOf;
    }

    public RevisionType getRevisionType() {
        return revisionType;
    }

    public void setRevisionType(RevisionType revisionType) {
        this.revisionType = revisionType;
    }

}
