package no.nav.aura.envconfig.model;

import com.google.common.base.Optional;
import no.nav.aura.envconfig.spring.User;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.io.Serializable;

@SuppressWarnings("serial")
@MappedSuperclass
public abstract class ModelEntity implements Serializable, Identifiable, Nameable {

    @Id
    @GeneratedValue()
    @Column(name = "entid")
    private Long id;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime created;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime updated;

    private String updatedBy;

    public ModelEntity() {
    }

    protected ModelEntity(ModelEntity orig) {
        this.id = orig.id;
        this.created = orig.created;
        this.updated = orig.updated;
        this.updatedBy = orig.updatedBy;
    }

    public Long getID() {
        return id;
    }

    public void setID(Long id) {
        this.id = id;
    }

    public boolean isNew() {
        return id == null;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public DateTime getUpdated() {
        return updated;
    }

    public void setUpdated(DateTime updated) {
        this.updated = updated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @PrePersist
    @PreUpdate
    protected void onMerge() {
        DateTime now = DateTime.now();

        if (isNew()) {
            setCreated(now);
        }

        String userName = User.getCurrentUser().getDisplayName();
        String ident = User.getCurrentUser().getIdentity();
        String authorlabel = ident == null ? userName : String.format("%s (%s)", userName, ident);

        setUpdated(now);

        setUpdatedBy(authorlabel);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    protected HashCodeBuilder createHashCodeBuilder() {
        return new HashCodeBuilder().append(id);
    }

    protected EqualsBuilder createEqualsBuilder(ModelEntity other) {
        return new EqualsBuilder().append(id, other.id);
    }

    @SuppressWarnings("unchecked")
    public <T extends ModelEntity> ModelEntityIdentifier<T, ?> getIdentifier() {
        return new ModelEntityIdentifier<>((Class<T>) this.getClass(), Optional.of(id), Optional.<Long> absent());
    }

}
