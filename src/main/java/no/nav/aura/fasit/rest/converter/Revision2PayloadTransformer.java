package no.nav.aura.fasit.rest.converter;

import java.net.URI;
import java.util.function.Function;

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.fasit.rest.model.RevisionPayload;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class Revision2PayloadTransformer<T extends ModelEntity> implements Function<FasitRevision<T>, RevisionPayload<T>> {

    private URI absolutePath;

    public Revision2PayloadTransformer(URI absolutePath) {
        this.absolutePath = absolutePath;

    }

    @Override
    public RevisionPayload<T> apply(FasitRevision<T> from) {    
        RevisionPayload<T> to = new RevisionPayload<>();
        to.timestamp = from.getTimestamp().toLocalDateTime();
        to.author = from.getAuthor();
        to.authorId = from.getAuthorId();
        to.message = from.getMessage();
        to.onbehalfOf = from.getOnbehalfOf();
        to.revision = from.getRevision();
        to.revisionType = from.getRevisionType();
//        URI revisionUri = UriBuilder.fromUri(absolutePath).path("{revision}").build(from.getRevision());
        URI revisionUri = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{revision}")
				.buildAndExpand(from.getRevision())
				.toUri();
        to.addLink("entity", revisionUri);

        return to;
    }

}
