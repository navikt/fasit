package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class LifecyclePayload {
    @NotNull(message = "status enum is required")
    public LifeCycleStatus status;
}
