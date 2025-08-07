package no.nav.aura.fasit.rest.model;

import javax.validation.constraints.NotNull;

public class ApplicationPayload  extends EntityPayload{
    
    @NotNull(message="application name is required")
    public String name;
    public String groupId;
    public String artifactId;
    public Integer portOffset;
    
    public ApplicationPayload() {
    }
    
    public ApplicationPayload(String name) {
        this.name = name;
    }

}
