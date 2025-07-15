package no.nav.aura.fasit.rest.converter;

import java.net.URI;

import org.springframework.web.util.UriComponentsBuilder;

import no.nav.aura.envconfig.model.application.Application;
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
        
        payload.addLink("self", UriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/applications/{name}")
                .buildAndExpand(application.getName())
                .toUri());
        
        payload.addLink("instances", UriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/applicationinstances")
                .queryParam("application", application.getName())
                .build()
                .toUri());
        
        payload.addLink("revisions", UriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/applications/{name}/revisions")
                .buildAndExpand(application.getName())
                .toUri());

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
