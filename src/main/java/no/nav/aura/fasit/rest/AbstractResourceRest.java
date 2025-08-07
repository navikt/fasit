package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.fasit.repository.*;
import no.nav.aura.fasit.rest.converter.Resource2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

public abstract class AbstractResourceRest {

    protected FasitRepository repo;
    protected ResourceRepository resourceRepository;
    protected ApplicationInstanceRepository applicationInstanceRepository;
    protected EnvironmentRepository environmentRepository;
    protected ApplicationRepository applicationRepository;
    protected ValidationHelpers validationHelpers;
    protected LifeCycleSupport lifeCycleSupport;
    protected RevisionRepository revisionRepository;

    @Context
    private UriInfo uriInfo;

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
        return new Resource2PayloadTransformer(repo, applicationInstanceRepository, uriInfo.getBaseUri());
    }

    protected Resource2PayloadTransformer createTransformer(Long currentRevision) {
        return new Resource2PayloadTransformer(repo, applicationInstanceRepository, uriInfo.getBaseUri(), currentRevision);
    }
    protected static <P> Optional<P> optional(P property) {
        return Optional.ofNullable(property);
    }
}
