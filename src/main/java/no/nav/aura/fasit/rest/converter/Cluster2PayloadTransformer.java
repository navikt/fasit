package no.nav.aura.fasit.rest.converter;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.Zone;
import no.nav.aura.fasit.rest.ApplicationInstanceRest;
import no.nav.aura.fasit.rest.ClusterRest;
import no.nav.aura.fasit.rest.NodesRest;
import no.nav.aura.fasit.rest.model.ClusterPayload;
import no.nav.aura.fasit.rest.model.Link;

public class Cluster2PayloadTransformer extends ToPayloadTransformer<Cluster, ClusterPayload> {

    private Environment environment;
    private URI baseUri;

    public Cluster2PayloadTransformer(UriInfo uriInfo, Environment environment) {
        this.baseUri = uriInfo.getBaseUri();

        this.environment = environment;
    }

    public Cluster2PayloadTransformer(UriInfo uriInfo, Environment environment, Long currentRevision) {
        this.baseUri = uriInfo.getBaseUri();
        this.environment = environment;
        this.revision = currentRevision;
    }

    @Override
    protected ClusterPayload transform(Cluster cluster) {
        ClusterPayload payload = new ClusterPayload();
        payload.id = cluster.getID();
        payload.addLink("self", ClusterRest.clusterUrl(baseUri, environment, cluster));
        payload.addLink("revisions", UriBuilder.fromUri(baseUri).path(ClusterRest.class).path(ClusterRest.class, "getRevisions").build(environment.getName(), cluster.getName()));

        payload.clusterName = cluster.getName();
        payload.environment = environment.getName();
        payload.environmentClass = environment.getEnvClass();
        payload.zone = cluster.getDomain().isInZone(Zone.FSS) ? Zone.FSS : Zone.SBS;
        payload.loadBalancerUrl = cluster.getLoadBalancerUrl();
        Set<Link> nodes = cluster.getNodes().stream()
                .map(n -> new Link(n.getHostname(), NodesRest.nodeUrl(baseUri, n.getHostname())))
                .collect(Collectors.toSet());
        payload.nodes = nodes;
        if (this.revision != null) {
            payload.revision = revision;
        }

        Set<Link> apps = cluster.getApplicationInstances().stream()
                .map(a -> new Link(
                        a.getApplication().getName(),
                        ApplicationInstanceRest.instanceUrl(baseUri, a.getID()))
                        .withId(a.getID()))
                .collect(Collectors.toSet());
        payload.applications = apps;

        return payload;

    }
}
