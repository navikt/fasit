package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityPayload {

    public Long id;
    public Long revision;
    public LocalDateTime created;
    public LocalDateTime updated;
    public String updatedBy;
    public final LifecyclePayload lifecycle = new LifecyclePayload();
    public final AccessControlPayload accessControl = new AccessControlPayload();
    public final Map<String, URI> links = new HashMap<>();

    public void addLink(String rel, URI uri) {
        links.put(rel, uri);
    }

    public static class AccessControlPayload {
        public EnvironmentClass environmentClass;
        public List<String> adGroups;
    }

}