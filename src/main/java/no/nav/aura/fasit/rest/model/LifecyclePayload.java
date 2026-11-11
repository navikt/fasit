package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;

import jakarta.validation.constraints.NotNull;

public class LifecyclePayload {
    // Case-insensitive parsing is handled by the EnumModule on the application's JsonMapper
    @NotNull(message = "status enum is required")
    public LifeCycleStatus status;

    public LifeCycleStatus getStatus() {
        return status;
    }

    public void setStatus(LifeCycleStatus status) {
        this.status = status;
    }
}
