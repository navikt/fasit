package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.repository.specs.EnvironmentSpecs;
import no.nav.aura.fasit.rest.converter.Environment2PayloadTransformer;
import no.nav.aura.fasit.rest.converter.Payload2EnvironmentTransformer;
import no.nav.aura.fasit.rest.converter.Revision2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;
import no.nav.aura.fasit.rest.model.EnvironmentPayload;
import no.nav.aura.fasit.rest.model.RevisionPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.aura.fasit.rest.helpers.PagingBuilder.pagingResponseBuilder;
import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;

@Component
@Path("api/v2/environments")
public class EnvironmentRest {

    @Inject
    private EnvironmentRepository environmentRepository;
    @Inject
    private RevisionRepository revisionRepository;
    @Inject
    private LifeCycleSupport lifeCycleSupport;
    @Inject
    private ValidationHelpers validationHelpers;

    private final static Logger log = LoggerFactory.getLogger(EnvironmentRest.class);

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findEnvironments(
            @QueryParam("environmentclass") EnvironmentClass environmentClass,
            @QueryParam("name") String name,
            @QueryParam("status") LifeCycleStatus lifeCycleStatus,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pr_page") @DefaultValue("100") int pr_page) {

        Specification<Environment> spec = EnvironmentSpecs.find(environmentClass, name, lifeCycleStatus);

        PageRequest pageRequest = new PageRequest(page, pr_page);
        Page<Environment> environments;

        if (spec != null) {
            environments = environmentRepository.findAll(spec, pageRequest);
        } else {
            environments = environmentRepository.findAll(pageRequest);
        }

        List<EnvironmentPayload> result = environments.getContent().stream()
                .map(new Environment2PayloadTransformer(uriInfo.getBaseUri()))
                .collect(Collectors.toList());

        return pagingResponseBuilder(environments, uriInfo.getRequestUri()).entity(result).build();
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentPayload getEnvironment(@PathParam("name") String environmentName) {
        Environment environment = validationHelpers.findEnvironment(environmentName);
        Long currentRevision = revisionRepository.currentRevision(Environment.class, environment.getID());
        return new Environment2PayloadTransformer(uriInfo.getBaseUri(), currentRevision).apply(environment);
    }

    @GET
    @Path("{name}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RevisionPayload<Environment>> getRevisions(
            @PathParam("name") String environmentName) {
        Environment environment = validationHelpers.findEnvironment(environmentName);
        List<FasitRevision<Environment>> revisions = revisionRepository.getRevisionsFor(Environment.class, environment.getID());

        List<RevisionPayload<Environment>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(uriInfo.getAbsolutePath()))
                .collect(Collectors.toList());
        return payload;
    }

    @GET
    @Path("{name}/revisions/{revision}")
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentPayload getEnvironmentByRevision(@PathParam("name") String environmentName, @PathParam("revision") Long revision) {
        Environment environment = validationHelpers.findEnvironment(environmentName);
        Optional<Environment> historic = revisionRepository.getRevisionEntry(Environment.class, environment.getID(), revision);
        Environment old = historic.orElseThrow(() -> new NotFoundException("Revison " + revision + " is not found for environment " + environmentName));
        return new Environment2PayloadTransformer(uriInfo.getBaseUri(), revision).apply(old);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createEnvironment(@Valid EnvironmentPayload payload) {
        Environment existingEnvironment = environmentRepository.findByNameIgnoreCase(payload.name);
        if (existingEnvironment != null) {
            throw new BadRequestException("Environment with name " + payload.name + " already exists");
        }

        Environment environment = new Payload2EnvironmentTransformer().apply(payload);
        checkAccess(environment);
        log.info("Creating new environment {}", environment.getName());
        environmentRepository.save(environment);
        URI environmentUri = UriBuilder.fromUri(uriInfo.getBaseUri()).path(EnvironmentRest.class).path(EnvironmentRest.class, "getEnvironment").build(environment.getName());
        return Response.created(environmentUri).build();
    }

    @PUT
    @Path("{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public EnvironmentPayload updateEnvironment(@PathParam("name") String environmentName, @Valid EnvironmentPayload payload) {
        Environment existingEnvironment = validationHelpers.findEnvironment(environmentName);
        checkAccess(existingEnvironment);
        if (!existingEnvironment.getEnvClass().equals(payload.environmentClass)) {
            throw new BadRequestException("It is not possible to change environmentclass on an environment. Existing :" + existingEnvironment.getEnvClass() + " new:" + payload.environmentClass);
        }
        Environment updatedEnvironment = new Payload2EnvironmentTransformer(existingEnvironment).apply(payload);
        lifeCycleSupport.update(updatedEnvironment, payload);
        log.info("Updating environment {}", environmentName);
        environmentRepository.save(updatedEnvironment);
        return new Environment2PayloadTransformer(uriInfo.getBaseUri()).apply(updatedEnvironment);
    }

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public void delete(@PathParam("name") String environmentName) {
        Environment existingEnvironment = validationHelpers.findEnvironment(environmentName);
        checkAccess(existingEnvironment);
        //TODO check if environment is empty?

        log.info("Updating environment {}", environmentName);
        lifeCycleSupport.delete(existingEnvironment);
        environmentRepository.delete(existingEnvironment);
    }
}
