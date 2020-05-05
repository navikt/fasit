package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Zone;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.util.Tuple;
import no.nav.aura.fasit.repository.*;
import no.nav.aura.fasit.repository.specs.ResourceSpecs;
import no.nav.aura.fasit.rest.converter.Resource2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;
import no.nav.aura.fasit.rest.model.ResourcePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
@Path("api/v2/scopedresource")
public class ScopedResourceRest extends AbstractResourceRest {

    @Context
    private UriInfo uriInfo;

    private final static Logger log = LoggerFactory.getLogger(ScopedResourceRest.class);

    @Inject
    public ScopedResourceRest(
            FasitRepository repo,
            ResourceRepository resourceRepository,
            ApplicationInstanceRepository applicationInstanceRepository,
            ValidationHelpers validationHelpers,
            RevisionRepository revisionRepository,
            LifeCycleSupport lifeCycleSupport) {
        super(repo, resourceRepository, applicationInstanceRepository, validationHelpers, lifeCycleSupport, revisionRepository);
    }

    /**
     * Finds best matching resource based on provided scope
     *
     * @responseMessage 404 No matches found
     * @responseMessage 400 If invalid or missing parameters
     * @responseMessage 400 If unable to decide which one is the best match
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResourcePayload findScopedResources(
            @QueryParam("alias") String alias,
            @QueryParam("type") ResourceType type,
            @QueryParam("environment") String environmentName,
            @QueryParam("application") String applicationName,
            @QueryParam("zone") Zone zone) {

        if (isEmpty(environmentName) || isEmpty(applicationName) || isEmpty(alias) || zone == null || type == null) {
            throw new BadRequestException("Missing required parameters. Required parameters are alias, type, application, environment, zone");
        }

        Environment environment = validationHelpers.getEnvironment(environmentName);
        Application application = validationHelpers.getApplication(applicationName);
        EnvironmentClass envClass = environment.getEnvClass();
        Domain domain = Domain.from(envClass, zone);

        Specification<Resource> spec = ResourceSpecs.findByScope(alias, type, envClass);

        List<Resource> initialSearch = resourceRepository.findAll(spec);

        List<Resource> filtered = initialSearch.stream()
                .filter(sameDomainOrNull(domain))
                .filter(sameEnvironmentOrNull(application))
                .filter(sameApplicationOrNull(environment))
                .collect(toList());

        if (filtered.isEmpty()) {
            throw new NotFoundException("No matching resources");
        }

        List<Tuple<Resource, Integer>> resourceWithWeight = filtered.stream().map(resource -> new Tuple<>(resource, resource.getScope().calculateScopeWeight())).collect(toList());
        Integer maxWeight = resourceWithWeight.stream().map(tuple -> tuple.snd).max(Integer::compare).get();

        List<Resource> bestMatchingResources = resourceWithWeight.stream().filter(tuple -> tuple.snd == maxWeight)
                .map(tuple -> tuple.fst)
                .collect(toList());

        if (bestMatchingResources.size() > 1) {
            throw new BadRequestException("Unable to decide which resource is the best match of the following " + bestMatchingResources);
        }
        Resource bestMatchingResource = bestMatchingResources.get(0);

        Long currentRevision = revisionRepository.currentRevision(Resource.class, bestMatchingResource.getID());
        Resource2PayloadTransformer transformer = createTransformer(currentRevision);

        return transformer.apply(bestMatchingResource);
    }

    private static Predicate<Resource> sameApplicationOrNull(Environment environment) {
        return resource -> resource.getScope().getEnvironmentName() == null || resource.getScope().getEnvironmentName().equals(environment.getName());
    }

    private static Predicate<Resource> sameEnvironmentOrNull(Application application) {
        return resource -> resource.getScope().getApplication() == null || resource.getScope().getApplication().equals(application);
    }

    private static Predicate<Resource> sameDomainOrNull(Domain domain) {
        return resource -> resource.getScope().getDomain() == null || resource.getScope().getDomain().equals(domain);
    }

}
