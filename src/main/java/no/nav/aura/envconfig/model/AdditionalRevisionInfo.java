package no.nav.aura.envconfig.model;

import no.nav.aura.envconfig.auditing.FasitRevisionListener;
import no.nav.aura.envconfig.auditing.NavUser;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
@Entity
@RevisionEntity(FasitRevisionListener.class)
public class AdditionalRevisionInfo<T extends ModelEntity> implements Serializable {

    private static final String ONBEHALFOF_SPLITCHAR = ";";

    @Id
    @GeneratedValue
    @RevisionNumber
    private long revision;

    @RevisionTimestamp
    private Date timestamp;

    private String author;
    private String authorId;
    private String message;
    private Class<?> modifiedEntityType;
    private String onBehalfOf;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getModifiedEntityType() {
        return (Class<T>) modifiedEntityType;
    }

    public void setModifiedEntityType(Class<T> modifiedEntityType) {
        this.modifiedEntityType = modifiedEntityType;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public DateTime getTimestamp() {
        return new DateTime(timestamp);
    }

    public long getRevision() {
        return revision;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public NavUser getOnBehalfOf() {
        if (onBehalfOf != null) {
            String[] split = onBehalfOf.split(ONBEHALFOF_SPLITCHAR);
            if (split.length == 2) {
                return new NavUser(split[0], split[1]);
            }
            return new NavUser(onBehalfOf);
        }
        return null;
    }

    public void setOnBehalfOf(NavUser onBehalfOf) {
        if (onBehalfOf != null) {
            if (onBehalfOf.getName() != null) {
                // if ID is UPN i.e firs.last@nav.no we do not need both email and full name.
                if(onBehalfOf.getId().contains("@")) {
                    this.onBehalfOf = onBehalfOf.getName();
                } else {
                    this.onBehalfOf = onBehalfOf.getId() + ONBEHALFOF_SPLITCHAR + onBehalfOf.getName();                }
            } else {
                this.onBehalfOf = onBehalfOf.getId();
            }
        }

    }
}
