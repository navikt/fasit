package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.envconfig.model.infrastructure.Zone;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
public class NodePayload extends EntityPayload {

        @NotNull(message="hostname is required")
        public String hostname;
        @NotNull(message="environmentClass is required")
        public EnvironmentClass environmentClass;
        @NotNull(message="environment is required")
        public String environment;
        @NotNull(message="type is required")
        public PlatformType type;

        public String username;
        @Valid
        public SecretPayload password;
        public Zone zone;

        public Set<Link> cluster = new HashSet<>();
        public  Set<String> applications = new HashSet<>();
        
        public NodePayload() {
        }
        
        public NodePayload(String hostname, EnvironmentClass environmentClass, String environment, PlatformType type) {
            this.hostname = hostname;
            this.environmentClass = environmentClass;
            this.environment = environment;
            this.type = type;
        }

    }
