package no.nav.aura.envconfig.model.deletion;

import jakarta.persistence.*;

import no.nav.aura.envconfig.model.ModelEntity;

import org.hibernate.envers.Audited;

@SuppressWarnings("serial")
@MappedSuperclass
@Audited
public abstract class DeleteableEntity extends ModelEntity {

    @Enumerated(EnumType.STRING)
    private LifeCycleStatus lifeCycleStatus;

    protected DeleteableEntity() {
        super();
    }

    protected DeleteableEntity(DeleteableEntity orig) {
        super(orig);
        this.lifeCycleStatus = orig.getLifeCycleStatus();
    }

    public void changeStatus(LifeCycleStatus changeToStatus) {
        this.lifeCycleStatus = changeToStatus;
    }

    public void resetStatus() {
        this.lifeCycleStatus = null;
    }

    public void setLifeCycleStatus(LifeCycleStatus changeToStatus) {
        this.lifeCycleStatus = changeToStatus;
    }

    public LifeCycleStatus getLifeCycleStatus() {
        return lifeCycleStatus;
    }
}
