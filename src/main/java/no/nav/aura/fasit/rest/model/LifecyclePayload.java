package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LifecyclePayload {
    @NotNull(message = "status enum is required")
    public LifeCycleStatus status;
    
    public LifeCycleStatus getStatus() {
        return status;
    }

    // Custom setter that handles case-insensitivity
    @JsonProperty("status")
    public void setStatus(String status) {
        if (status == null) {
            this.status = null;
            return;
        }
        
        for (LifeCycleStatus s : LifeCycleStatus.values()) {
            if (s.name().equalsIgnoreCase(status)) {
                this.status = s;
                return;
            }
        }
        throw new IllegalArgumentException("Invalid status: " + status);
    }
}
