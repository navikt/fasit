package no.nav.aura.fasit.rest.converter;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.fasit.rest.ClusterRest;
import no.nav.aura.fasit.rest.EnvironmentRest;
import no.nav.aura.fasit.rest.NodesRest;
import no.nav.aura.fasit.rest.model.EnvironmentPayload;

public class Environment2PayloadTransformer extends ToPayloadTransformer<Environment, EnvironmentPayload> {

    private URI baseUri;

    public Environment2PayloadTransformer(URI baseUri) {
        this.baseUri = baseUri;
    }
    public Environment2PayloadTransformer(URI baseUri, Long currentRevision) {
        this.baseUri = baseUri;
        this.revision = currentRevision;
    }

    @Override
    protected EnvironmentPayload transform(Environment environment) {
        EnvironmentPayload payload = new EnvironmentPayload();
        payload.addLink("self", UriBuilder.fromUri(baseUri).path(EnvironmentRest.class).path(EnvironmentRest.class, "getEnvironment").build(environment.getName()));
        payload.addLink("revisions", UriBuilder.fromUri(baseUri).path(EnvironmentRest.class).path(EnvironmentRest.class, "getRevisions").build(environment.getName()));
        payload.addLink("clusters", UriBuilder.fromUri(baseUri).path(ClusterRest.class).build(environment.getName()));
        payload.addLink("nodes", UriBuilder.fromUri(baseUri).path(NodesRest.class).queryParam("environment",environment.getName()).build());

        if (revision != null){
            payload.revision = revision;
        }
        payload.name=environment.getName();
        payload.environmentClass=environment.getEnvClass();

        return payload;

    }
}
