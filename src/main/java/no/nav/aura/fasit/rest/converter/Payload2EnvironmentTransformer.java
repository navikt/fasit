package no.nav.aura.fasit.rest.converter;

import java.util.Optional;

import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.fasit.rest.model.EnvironmentPayload;

public class Payload2EnvironmentTransformer extends FromPayloadTransformer<EnvironmentPayload, Environment> {
    
    private Optional<Environment> existing;
    
    public Payload2EnvironmentTransformer() {
        this(null);
    }
    
    public Payload2EnvironmentTransformer(Environment existing) {
        this.existing= Optional.ofNullable(existing);
    }
    

    @Override
    protected Environment transform(EnvironmentPayload from) {
        Environment environment = existing.orElse(new Environment(from.name, from.environmentClass));
        environment.setName(from.name);
        return environment;
    }
}
