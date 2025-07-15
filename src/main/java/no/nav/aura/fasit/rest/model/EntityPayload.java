package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityPayload {

    public Long id;
    public Long revision;
    public LocalDateTime created;
    public LocalDateTime updated;
    @JsonProperty("updatedby")
    public String updatedBy;
    public final LifecyclePayload lifecycle = new LifecyclePayload();
    @JsonProperty("accesscontrol")
    public final AccessControlPayload accessControl = new AccessControlPayload();
    public final Map<String, URI> links = new HashMap<>();

    public void addLink(String rel, URI uri) {
        links.put(rel, uri);
    }

    public static class AccessControlPayload {
    	@JsonProperty("environmentclass")
        public EnvironmentClass environmentClass;
    	@JsonProperty("adgroups")
        public List<String> adGroups;
    }

}