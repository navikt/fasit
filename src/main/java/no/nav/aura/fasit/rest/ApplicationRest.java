package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.repository.specs.ApplicationInstanceSpecs;
import no.nav.aura.fasit.repository.specs.ApplicationSpecs;
import no.nav.aura.fasit.rest.converter.Application2PayloadTransformer;
import no.nav.aura.fasit.rest.converter.Payload2ApplicationTransformer;
import no.nav.aura.fasit.rest.converter.Revision2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;
import no.nav.aura.fasit.rest.model.ApplicationPayload;
import no.nav.aura.fasit.rest.model.RevisionPayload;
import no.nav.aura.fasit.rest.security.AccessChecker;
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
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.aura.fasit.rest.helpers.PagingBuilder.pagingResponseBuilder;
import static no.nav.aura.fasit.rest.security.AccessChecker.*;
import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;

@Component
@Path("api/v2/applications")
public class ApplicationRest {

    @Inject
    private ApplicationRepository applicationRepository;
    @Inject
    private ApplicationInstanceRepository applicationInstanceRepository;
    @Inject
    private RevisionRepository revisionRepository;

    @Inject
    private ValidationHelpers validationHelpers;

    private final static Logger log = LoggerFactory.getLogger(ApplicationRest.class);

    @Inject
    private LifeCycleSupport lifeCycleSupport;

    @Context
    private UriInfo uriInfo;

    public ApplicationRest() {
    }

    /**
     * Find a list of applications
     *
     * @param name filter application name containing
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findApplications(@QueryParam("name") String name,
                                     @QueryParam("status") LifeCycleStatus lifeCycleStatus,
                                     @QueryParam("page") @DefaultValue("0") int page,
                                     @QueryParam("pr_page") @DefaultValue("100") int pr_page) {

        Specification<Application> spec = ApplicationSpecs.find(name, lifeCycleStatus);
        PageRequest pageRequest = new PageRequest(page, pr_page);

        Page<Application> applications;

        if (spec != null){
            applications = applicationRepository.findAll(spec, pageRequest);
        } else {
            applications = applicationRepository.findAll(pageRequest);
        }

        List<ApplicationPayload> result = applications.getContent().stream()
                .map(new Application2PayloadTransformer(uriInfo.getBaseUri()))
                .collect(Collectors.toList());

        return pagingResponseBuilder(applications, uriInfo.getRequestUri()).entity(result).build();
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationPayload getApplication(@PathParam("name") String applicationName) {
        Application application = validationHelpers.findApplication(applicationName);
        Long currentRevision = revisionRepository.currentRevision(Application.class, application.getID());

        return new Application2PayloadTransformer(uriInfo.getBaseUri(), currentRevision).apply(application);
    }

    @GET
    @Path("{name}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RevisionPayload<Application>> getRevisions(
            @PathParam("name") String applicationName) {
        Application application = validationHelpers.findApplication(applicationName);
        List<FasitRevision<Application>> revisions = revisionRepository.getRevisionsFor(Application.class, application.getID());

        List<RevisionPayload<Application>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(uriInfo.getAbsolutePath()))
                .collect(Collectors.toList());
        return payload;
    }

    @GET
    @Path("{name}/revisions/{revision}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationPayload getApplicationByRevision(@PathParam("name") String applicationName, @PathParam("revision") Long revision) {
        Application application = validationHelpers.findApplication(applicationName);
        Optional<Application> historic = revisionRepository.getRevisionEntry(Application.class, application.getID(), revision);
        Application old = historic.orElseThrow(() -> new NotFoundException("Revison " + revision + " is not found for application " + applicationName));
        return new Application2PayloadTransformer(uriInfo.getBaseUri(), revision).apply(old);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createApplication(@Valid ApplicationPayload payload) {
        Application existing = applicationRepository.findByNameIgnoreCase(payload.name);
        if (existing != null) {
            throw new BadRequestException("Application with name " + payload.name + " allready exists");
        }
        Application application = new Payload2ApplicationTransformer().apply(payload);
        checkSuperuserAccess();

        log.info("Creating new application {}", application.getName());
        applicationRepository.save(application);
        URI environmentUri = UriBuilder.fromUri(uriInfo.getBaseUri()).path(ApplicationRest.class).path(ApplicationRest.class, "getApplication").build(application.getName());
        return Response.created(environmentUri).build();
    }

    @PUT
    @Path("{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public ApplicationPayload updateApplication(@PathParam("name") String applicationName, @Valid ApplicationPayload payload) {
        Application existing = validationHelpers.findApplication(applicationName);
        if (!existing.getName().equals(payload.name)) {
            throw new BadRequestException("It is not possible to change name of an application. Delete it and create a new one. Existing :" + existing.getName() + " new:" + payload.name);
        }
        checkAccess(existing);
        validatePortConflicts(existing, payload);
        Application application = new Payload2ApplicationTransformer(existing).apply(payload);
        lifeCycleSupport.update(existing, payload);
        log.info("Updating application {}", application.getName());
        applicationRepository.save(application);
        return new Application2PayloadTransformer(uriInfo.getBaseUri()).apply(application);
    }


    @DELETE
    @Path("{name}")
    @Transactional
    public void deleteApplication(@PathParam("name") String applicationName) {
        Application application = validationHelpers.findApplication(applicationName);
        checkAccess(application);
        validateNoDeployedInstancesOnDelete(application);
        lifeCycleSupport.delete(application);
        log.info("Deleting application {}", application.getName());
        applicationRepository.delete(application);
    }

    private void validateNoDeployedInstancesOnDelete(Application application) {
        List<ApplicationInstance> instances = applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByApplicationName(application.getName()));
        long deployedCount = instances.stream()
                .filter(ai -> ai.isDeployed())
                .count();

        if (deployedCount > 0) {
            throw new BadRequestException("Application " + application.getName() + " can not be deleted because it is deployed to " + deployedCount + " environment(s)");
        }
    }

    private void validatePortConflicts(Application existingApplication, ApplicationPayload payload) {
        int portOffset = payload.portOffset;
        if (portOffset != existingApplication.getPortOffset()) {
            List<ApplicationInstance> instances = applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByApplicationName(existingApplication.getName()));

            Set<Application> otherAppsInSameClusters = instances.stream()
                    .flatMap(i -> i.getCluster().getApplicationInstances().stream())
                    .map(i -> i.getApplication())
                    .filter(a -> !a.getName().equals(existingApplication.getName()))
                    .collect(Collectors.toSet());

            Optional<Application> conflicting = otherAppsInSameClusters.stream()
                    .filter(a -> a.getPortOffset() == portOffset)
                    .findFirst();

            if (conflicting.isPresent()) {
                String usedPorts = otherAppsInSameClusters.stream()
                        .map(app -> String.valueOf(app.getPortOffset()))
                        .collect(Collectors.joining(",  "));

                throw new BadRequestException("Conflicting portoffset with application " + conflicting.get().getName() + " mapped to the same cluster as this application. You can't use ports " + usedPorts);
            }

        }

    }
}
