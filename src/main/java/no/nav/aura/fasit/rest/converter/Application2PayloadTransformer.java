package no.nav.aura.fasit.rest.converter;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.fasit.rest.ApplicationInstanceRest;
import no.nav.aura.fasit.rest.ApplicationRest;
import no.nav.aura.fasit.rest.model.ApplicationPayload;

public class Application2PayloadTransformer extends ToPayloadTransformer<Application, ApplicationPayload> {

    private URI baseUri;

    public Application2PayloadTransformer(URI baseUri) {
        this.baseUri = baseUri;
    }
    public Application2PayloadTransformer(URI baseUri, Long currentRevision) {
        this.baseUri = baseUri;
        this.revision = currentRevision;
    }

    @Override
    protected ApplicationPayload transform(Application application) {
        ApplicationPayload payload = new ApplicationPayload();
        payload.addLink("self", UriBuilder.fromUri(baseUri).path(ApplicationRest.class).path(ApplicationRest.class, "getApplication").build(application.getName()));
        payload.addLink("instances", UriBuilder.fromUri(baseUri).path(ApplicationInstanceRest.class).path(ApplicationInstanceRest.class, "findApplicationInstancesByApplication").build(application.getName()));
        payload.addLink("revisions", UriBuilder.fromUri(baseUri).path(ApplicationRest.class).path(ApplicationRest.class, "getRevisions").build(application.getName()));
        if (revision != null){
            payload.revision=revision;
        }
        payload.name = application.getName();
        payload.portOffset = application.getPortOffset();
        payload.artifactId = application.getArtifactId();
        payload.groupId = application.getGroupId();

        return payload;
    }
}
