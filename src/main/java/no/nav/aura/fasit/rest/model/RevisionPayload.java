package no.nav.aura.fasit.rest.model;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.envers.RevisionType;

import no.nav.aura.envconfig.auditing.NavUser;

public class RevisionPayload<T> {
    public long revision;
    public LocalDateTime timestamp;
    public String author;
    @JsonProperty("onbehalfof")
    public NavUser onbehalfOf;
    @JsonProperty("authorid")
    public String authorId;
    public String message;
    @JsonProperty("revisiontype")
    public RevisionType revisionType;
    public final Map<String, URI> links = new HashMap<>();
    
    public void addLink(String rel, URI uri){
        links.put(rel, uri);
    }
    
 
}
