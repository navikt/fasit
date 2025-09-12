package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.fasit.rest.model.PortPayload.PortType;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class ApplicationInstancePayload extends EntityPayload {
    @NotNull(message = "application name is required")
    public String application;
    @NotNull(message = "environment is required")
    public String environment;
    @NotNull(message = "version is required")
    public String version;
    @Valid
    public Set<NodeRefPayload> nodes = new HashSet<>();

    public URI loadbalancerurl;

    public String selftest;
    public Set<String> selfTestUrls = new HashSet();
    public Link cluster;
    public String clusterName;
    public String domain;

    @Valid
    public AppconfigPayload appconfig;

    @Valid
    public Set<ResourceRefPayload> exposedresources = new HashSet<>();
    @Valid
    public Set<ResourceRefPayload> usedresources = new HashSet<>();
    @Valid
    public Set<MissingResourcePayload> missingresources = new HashSet<>();
    public EnvironmentClass environmentClass;

    public ApplicationInstancePayload() {
    }

    public ApplicationInstancePayload(Boolean showUsage) {
        if (!showUsage) {
            exposedresources = null;
            usedresources = null;
            missingresources = null;
        }
    }

    public ApplicationInstancePayload(String application, String environment) {
        this.application = application;
        this.environment = environment;
    }

    public static class AppconfigPayload {
        @NotNull(message = "value is required")
        public String value;
        public URI ref;

        public AppconfigPayload(String value) {
            this.value = value;
        }

        public AppconfigPayload(URI ref) {
            this.ref = ref;
        }
    }

    public static class NodeRefPayload {
        @NotNull(message = "hostname of node is required")
        public String hostname;
        @Size(min = 1, message = "A node must have at least one port")
        public Set<PortPayload> ports = new HashSet<>();

        public NodeRefPayload(String hostname, int port, PortType type) {
            this.hostname = hostname;
            this.ports.add(new PortPayload(port, type));
        }

        public NodeRefPayload(String hostname, Set<PortPayload> ports) {
            this.hostname = hostname;
            this.ports = ports;
        }

        @Override
        public String toString() {
            return hostname + " : " + ports;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(hostname).build();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof NodeRefPayload)) {
                return false;
            }

            return ((NodeRefPayload) other).hostname.equalsIgnoreCase(hostname);
        }
    }

    public static class ResourceRefPayload {
        @NotNull(message = "id of resource is required")
        public Long id;
        public Long revision;
        public String alias;
        public ResourceType type;
        public ScopePayload scope;
        public URI ref;
        public Boolean deleted;
        public Long lastChange;
        public String lastUpdateBy;

        public ResourceRefPayload() {
        }

        public ResourceRefPayload(long id) {
            this.id = id;
        }

        public ResourceRefPayload(long id, long revision) {
            this.id = id;
            this.revision = revision;
        }

        @Override
        public String toString() {
            return id + ":" + revision;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(id).build();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ResourceRefPayload)) {
                return false;
            }

            ResourceRefPayload o = (ResourceRefPayload) other;


            if (o.deleted != null && o.deleted == true) {
                return o.alias.compareTo(alias) == 0;
            }

            if (o.revision == null) {
                return o.id.compareTo(id) == 0;
            }

            return o.id.compareTo(id) == 0 && o.revision.compareTo(revision) == 0;

        }
    }

        public static class MissingResourcePayload {
            @NotNull(message = "resource alias is required")
            public String alias;
            @NotNull(message = "type is required")
            public ResourceType type;

            public MissingResourcePayload(String alias, ResourceType type) {
                this.alias = alias;
                this.type = type;
            }

        }

    }
