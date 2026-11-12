package no.nav.aura.fasit.rest.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Zone;

public class ClusterPayload extends EntityPayload {
    @NotNull(message="clustername is required")
    @JsonProperty("clustername")
    public String clusterName;
    @NotNull(message="zone is required")
    public Zone zone;
    public String environment;
    @JsonProperty("environmentclass")
    public EnvironmentClass environmentClass;
    @JsonProperty("loadbalancerurl")
    public String loadBalancerUrl;
    @Valid
    public Set<Link> nodes;
    @Valid
    public Set<Link> applications;
    
    public ClusterPayload() {
    }
    
    public ClusterPayload(String clusterName, Zone zone) {
        this.clusterName = clusterName;
        this.zone = zone;
        this.nodes= new HashSet<>();
        this.applications= new HashSet<>();
    }
}
