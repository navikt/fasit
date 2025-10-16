package no.nav.aura.fasit.rest.model;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.fasit.rest.SecretRest;

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
    @JsonProperty("lifecyclestatus")
    public LifeCycleStatus lifeCycleStatus;
    @JsonProperty("exposedby")
    public UsedApplicationInstancePayload exposedBy;
    @JsonProperty("usedbyapplications")
    public List<UsedApplicationInstancePayload> usedByApplications;
    public boolean dodgy;

    public ResourcePayload() {
    }

    public ResourcePayload(ResourceType type, String alias) {
        this.type = type;
        this.alias = alias;
    }

    public ResourcePayload(@NotNull(message = "resource type is required") ResourceType type,
			@NotNull(message = "alias is required") String alias,
			@Valid @NotNull(message = "scope is required") ScopePayload scope, Map<String, String> properties,
			@Valid Map<String, SecretPayload> secrets, @Valid Map<String, FilePayload> files,
			LifeCycleStatus lifeCycleStatus, UsedApplicationInstancePayload exposedBy,
			List<UsedApplicationInstancePayload> usedByApplications, boolean dodgy) {
		super();
		this.type = type;
		this.alias = alias;
		this.scope = scope;
		this.properties = properties;
		this.secrets = secrets;
		this.files = files;
		this.lifeCycleStatus = lifeCycleStatus;
		this.exposedBy = exposedBy;
		this.usedByApplications = usedByApplications;
		this.dodgy = dodgy;
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

    public ResourceType getType() {
		return type;
	}

	public void setType(ResourceType type) {
		this.type = type;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public ScopePayload getScope() {
		return scope;
	}

	public void setScope(ScopePayload scope) {
		this.scope = scope;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public Map<String, SecretPayload> getSecrets() {
		return secrets;
	}

	public void setSecrets(Map<String, SecretPayload> secrets) {
		this.secrets = secrets;
	}

	public Map<String, FilePayload> getFiles() {
		return files;
	}

	public void setFiles(Map<String, FilePayload> files) {
		this.files = files;
	}

	public LifeCycleStatus getLifeCycleStatus() {
		return lifeCycleStatus;
	}

	public void setLifeCycleStatus(LifeCycleStatus lifeCycleStatus) {
		this.lifeCycleStatus = lifeCycleStatus;
	}

	public UsedApplicationInstancePayload getExposedBy() {
		return exposedBy;
	}

	public void setExposedBy(UsedApplicationInstancePayload exposedBy) {
		this.exposedBy = exposedBy;
	}

	public List<UsedApplicationInstancePayload> getUsedByApplications() {
		return usedByApplications;
	}

	public void setUsedByApplications(List<UsedApplicationInstancePayload> usedByApplications) {
		this.usedByApplications = usedByApplications;
	}

	public boolean isDodgy() {
		return dodgy;
	}

	public void setDodgy(boolean dodgy) {
		this.dodgy = dodgy;
	}




	public static class UsedApplicationInstancePayload {
        public String application;
        public String environment;
        public String version;
        public Long id;
        public URI ref;

		public UsedApplicationInstancePayload() {
		}

		public UsedApplicationInstancePayload(String application, String environment, String version, Long id,
				URI ref) {
			this.application = application;
			this.environment = environment;
			this.version = version;
			this.id = id;
			this.ref = ref;
		}

		public String getApplication() {
			return application;
		}

		public void setApplication(String application) {
			this.application = application;
		}

		public String getEnvironment() {
			return environment;
		}

		public void setEnvironment(String environment) {
			this.environment = environment;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public URI getRef() {
			return ref;
		}

		public void setRef(URI ref) {
			this.ref = ref;
		}
    }

    public static class FilePayload {
        public String filename;
        public URI ref;
        @JsonProperty("filecontent")
        public String fileContent;

        public FilePayload() {
		}

        public FilePayload(String filename, URI ref, String fileContent) {
			this.filename = filename;
			this.ref = ref;
			this.fileContent = fileContent;
		}

		public FilePayload(String filename, URI fileref) {
            this.filename = filename;
            this.ref = fileref;
        }

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public URI getRef() {
			return ref;
		}

		public void setRef(URI ref) {
			this.ref = ref;
		}

		public String getFileContent() {
			return fileContent;
		}

		public void setFileContent(String fileContent) {
			this.fileContent = fileContent;
		}
    }
}
