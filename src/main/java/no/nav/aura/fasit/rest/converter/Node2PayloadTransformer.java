package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.fasit.rest.ClusterRest;
import no.nav.aura.fasit.rest.NodesRest;
import no.nav.aura.fasit.rest.model.Link;
import no.nav.aura.fasit.rest.model.NodePayload;
import no.nav.aura.fasit.rest.model.SecretPayload;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class Node2PayloadTransformer extends ToPayloadTransformer<Node, NodePayload> {

    private NodeRepository nodeRepository;
    private URI baseUri;

    public Node2PayloadTransformer(NodeRepository nodeRepository, URI baseUri) {
        this.nodeRepository = nodeRepository;
        this.baseUri = baseUri;
    }

    public Node2PayloadTransformer(NodeRepository nodeRepository, URI baseUri, Long currentRevision) {
        this.nodeRepository = nodeRepository;
        this.baseUri = baseUri;
        this.revision = currentRevision;
    }

    private Zone getZone(Node node) {
        try {
            Domain domain = node.getDomain();
            return domain.getZone().get(0);

        } catch (IllegalArgumentException iae) {
            // Intentionally skipping this to avoid crashing transformer if node does not have a valid node. F.ex if node is registered with IP
            return null;
        }
    }


    @Override
    protected NodePayload transform(Node node) {
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUri).path(NodesRest.class).path("{hostname}");
        NodePayload payload = new NodePayload();
        payload.addLink("self", uriBuilder.build(node.getHostname()));
        payload.addLink("revisions", uriBuilder.clone().path("revisions").build(node.getHostname()));
        payload.username = node.getUsername();
        payload.hostname = node.getHostname();
        payload.type = node.getPlatformType();
        payload.zone = getZone(node);

        if (node.getPassword() != null && !node.getPassword().isNew()) {
            payload.password = SecretPayload.withIdAndBaseUri(node.getPassword().getID(), baseUri);
        }
        if (revision != null) {
            payload.revision = revision;
        }
        Environment environment = nodeRepository.findEnvironment(node);
        if (environment != null) {
            payload.environment = environment.getName();
            payload.environmentClass = environment.getEnvClass();
        }
        payload.cluster = node
                .getClusters()
                .stream()
                .map(cluster -> new Link(cluster.getName(), ClusterRest.clusterUrl(baseUri, environment, cluster)))
                .collect(toSet());

        Set<String> applicationsAcrossAllClustersOnNode = node.getClusters()
                .stream()
                .map(Cluster::getApplications)
                .flatMap(Collection::stream)
                .map(a -> a.getName())
                .collect(toSet());
        payload.applications = applicationsAcrossAllClustersOnNode;

        return payload;
    }
}
