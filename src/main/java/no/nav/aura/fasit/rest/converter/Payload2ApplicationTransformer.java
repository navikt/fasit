package no.nav.aura.fasit.rest.converter;

import java.util.Optional;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.fasit.rest.model.ApplicationPayload;

public class Payload2ApplicationTransformer extends FromPayloadTransformer<ApplicationPayload, Application> {

    private Optional<Application> existing;

    public Payload2ApplicationTransformer() {
        this(null);
    }

    public Payload2ApplicationTransformer(Application existing) {
        this.existing = Optional.ofNullable(existing);
    }

    @Override
    protected Application transform(ApplicationPayload from) {
        Application application = existing.orElse(new Application(from.name));
        
        optional(from.artifactId).ifPresent(prop -> application.setArtifactId(prop));
        optional(from.groupId).ifPresent(prop -> application.setGroupId(prop));
        optional(from.portOffset).ifPresent(prop -> application.setPortOffset(prop));
        return application;
    }
}
