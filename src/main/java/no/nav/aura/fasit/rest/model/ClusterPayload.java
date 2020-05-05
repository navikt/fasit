package no.nav.aura.fasit.rest.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Zone;

public class ClusterPayload extends EntityPayload {
    @NotNull(message="clustername is required")
    public String clusterName;
    @NotNull(message="zone is required")
    public Zone zone;
    public String environment;
    public EnvironmentClass environmentClass;
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
