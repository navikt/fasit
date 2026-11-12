package no.nav.aura.envconfig.model.deletion;

import org.hibernate.envers.Audited;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import no.nav.aura.envconfig.model.ModelEntity;

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
