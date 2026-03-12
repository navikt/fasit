package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.envconfig.model.infrastructure.Zone;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;
public class NodePayload extends EntityPayload {

        @NotNull(message="hostname is required")
        public String hostname;
        @NotNull(message="environmentClass is required")
        @JsonProperty("environmentclass")
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

		public String getHostname() {
			return hostname;
		}

		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		public EnvironmentClass getEnvironmentClass() {
			return environmentClass;
		}

		public void setEnvironmentClass(EnvironmentClass environmentClass) {
			this.environmentClass = environmentClass;
		}

		public String getEnvironment() {
			return environment;
		}

		public void setEnvironment(String environment) {
			this.environment = environment;
		}

		public PlatformType getType() {
			return type;
		}

		public void setType(PlatformType type) {
			this.type = type;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public SecretPayload getPassword() {
			return password;
		}

		public void setPassword(SecretPayload password) {
			this.password = password;
		}

		public Zone getZone() {
			return zone;
		}

		public void setZone(Zone zone) {
			this.zone = zone;
		}

		public Set<Link> getCluster() {
			return cluster;
		}

		public void setCluster(Set<Link> cluster) {
			this.cluster = cluster;
		}

		public Set<String> getApplications() {
			return applications;
		}

		public void setApplications(Set<String> applications) {
			this.applications = applications;
		}
        
    }
