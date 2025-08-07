package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.fasit.rest.SecretRest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourcePayload extends EntityPayload {
    @NotNull(message = "resource type is required")
    public ResourceType type;
    @NotNull(message = "alias is required")
    public String alias;
    @Valid
    @NotNull(message = "scope is required")
    public ScopePayload scope;
    public Map<String, String> properties = new HashMap<>();
    @Valid
    public Map<String, SecretPayload> secrets = new HashMap<>();
    @Valid
    public Map<String, FilePayload> files = new HashMap<>();
    public LifeCycleStatus lifeCycleStatus;
    public UsedApplicationInstancePayload exposedBy;
    public List<UsedApplicationInstancePayload> usedByApplications;
    public boolean dodgy;

    public ResourcePayload() {
    }

    public ResourcePayload(ResourceType type, String alias) {
        this.type = type;
        this.alias = alias;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public void addSecret(String propertyName, Long id, URI baseUri, String vaultPath) {
        if (id != null) {
            URI ref = SecretRest.secretUri(baseUri, id);
            SecretPayload payload = SecretPayload.withURI(ref);
            payload.vaultpath = vaultPath;
            secrets.put(propertyName, payload);
        }
    }


    public void addFile(String propertyName, URI fileUri) {
        files.put(propertyName, new FilePayload(propertyName, fileUri));
    }

    public static class UsedApplicationInstancePayload {
        public String application;
        public String environment;
        public String version;
        public Long id;
        public URI ref;
    }

    public static class FilePayload {
        public String filename;
        public URI ref;
        public String fileContent;

        public FilePayload(String filename, URI fileref) {
            this.filename = filename;
            this.ref = fileref;
        }
    }
}
