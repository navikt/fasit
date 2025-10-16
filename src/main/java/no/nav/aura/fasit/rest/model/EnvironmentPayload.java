package no.nav.aura.fasit.rest.model;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnvironmentPayload extends EntityPayload {

    @NotNull(message = "name is required")
    public String name;
    @NotNull(message = "environmentclass is required")
    @JsonProperty("environmentclass")
    public EnvironmentClass environmentClass;
  
    public EnvironmentPayload() {
    }
    
    public EnvironmentPayload(String name, EnvironmentClass environmentClass){
        this.name = name;
        this.environmentClass = environmentClass;
    }
}
