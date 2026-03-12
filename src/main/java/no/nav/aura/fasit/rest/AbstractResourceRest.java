package no.nav.aura.fasit.rest;

import java.net.URI;
import java.util.Optional;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.rest.converter.Resource2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;

public abstract class AbstractResourceRest {

    protected FasitRepository repo;
    protected ResourceRepository resourceRepository;
    protected ApplicationInstanceRepository applicationInstanceRepository;
    protected EnvironmentRepository environmentRepository;
    protected ApplicationRepository applicationRepository;
    protected ValidationHelpers validationHelpers;
    protected LifeCycleSupport lifeCycleSupport;
    protected RevisionRepository revisionRepository;


    public AbstractResourceRest(
            FasitRepository repo,
            ResourceRepository resourceRepository,
            ApplicationInstanceRepository applicationInstanceRepository,
            ValidationHelpers validationHelpers,
            LifeCycleSupport lifeCycleSupport,
            RevisionRepository revisionRepository
    ) {

        this.repo = repo;
        this.resourceRepository = resourceRepository;
        this.applicationInstanceRepository = applicationInstanceRepository;
        this.validationHelpers = validationHelpers;
        this.lifeCycleSupport = lifeCycleSupport;
        this.revisionRepository = revisionRepository;
    }

    protected Resource2PayloadTransformer createTransformer() {
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return new Resource2PayloadTransformer(repo, applicationInstanceRepository, baseUri);
    }

    protected Resource2PayloadTransformer createTransformer(Long currentRevision) {
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return new Resource2PayloadTransformer(repo, applicationInstanceRepository, baseUri, currentRevision);
    }
    protected static <P> Optional<P> optional(P property) {
        return Optional.ofNullable(property);
    }
}
