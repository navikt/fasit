package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.repository.specs.ApplicationInstanceSpecs;
import no.nav.aura.fasit.rest.converter.ApplicationInstance2PayloadTransformer;
import no.nav.aura.fasit.rest.converter.Payload2ApplicationInstanceTransformer;
import no.nav.aura.fasit.rest.converter.Revision2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.PagingBuilder;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload.ResourceRefPayload;
import no.nav.aura.fasit.rest.model.RevisionPayload;
import no.nav.aura.fasit.rest.security.AccessChecker;
import no.nav.aura.integration.FasitKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Component
@Path("api/v2/applicationinstances")
public class ApplicationInstanceRest {
    @Inject
    private ApplicationInstanceRepository applicationInstanceRepository;
    @Inject
    private ResourceRepository resourceRepository;
    @Inject
    private RevisionRepository revisionRepository;
    @Inject
    private LifeCycleSupport lifeCycleSupport;
    @Inject
    private ValidationHelpers validationHelpers;
    @Inject
    private EnvironmentRepository environmentRepository;
    @Inject
    private ResourceRest resourceRest;
    @Inject
    private FasitKafkaProducer kafkaProducer;

    private static final Logger log = LoggerFactory.getLogger(ApplicationInstanceRest.class);

    @Context
    private UriInfo uriInfo;

    public ApplicationInstanceRest() {
    }

    public ApplicationInstanceRest(ApplicationInstanceRepository applicationInstanceRepository, RevisionRepository revisionRepository, ResourceRepository resourceRepository, ResourceRest resourceRest) {
        this.applicationInstanceRepository = applicationInstanceRepository;
        this.revisionRepository = revisionRepository;
        this.resourceRepository = resourceRepository;
        this.resourceRest = resourceRest;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findApplicationInstances(@QueryParam("environment") String environmentName,
                                             @QueryParam("environmentclass") EnvironmentClass environmentClass,
                                             @QueryParam("application") String applicationName,
                                             @QueryParam("status") LifeCycleStatus lifeCycleStatus,
                                             @QueryParam("usage") @DefaultValue("true") Boolean showUsage,
                                             @QueryParam("page") @DefaultValue("0") int page,
                                             @QueryParam("pr_page") @DefaultValue("100") int pr_page) {
        Specification<ApplicationInstance> spec = ApplicationInstanceSpecs.find(environmentName, environmentClass, applicationName, lifeCycleStatus);

        Page<ApplicationInstance> result = findApplicationInstancesBy(spec, page, pr_page);
        List<ApplicationInstancePayload> payload = createPayload(result, showUsage);

        return PagingBuilder.pagingResponseBuilder(result, uriInfo.getRequestUri()).entity(payload).build();
    }

    @GET
    @Path("environment/{environment}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findApplicationInstancesByEnvironment(@PathParam("environment") String environmentName,
                                                          @QueryParam("usage") @DefaultValue("true") Boolean showUsage,
                                                          @QueryParam("page") @DefaultValue("0") int page,
                                                          @QueryParam("pr_page") @DefaultValue("100") int pr_page) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Specification<ApplicationInstance> spec = ApplicationInstanceSpecs.findByEnvironment(environment);
        Page<ApplicationInstance> result = findApplicationInstancesBy(spec, page, pr_page);
        List<ApplicationInstancePayload> payload = createPayload(result, showUsage);

        return PagingBuilder.pagingResponseBuilder(result, uriInfo.getRequestUri()).entity(payload).build();
    }

    @GET
    @Path("environment/{environment}/application/{application}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationInstancePayload findAppInstanceByEnvAndApp(@PathParam("environment") String environmentName,
                                                                 @PathParam("application") String applicationName) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Application application = validationHelpers.getApplication(applicationName);

        Specification<ApplicationInstance> spec = ApplicationInstanceSpecs.findByEnvironmentAndApplication(environment, application);
        ApplicationInstance applicationInstance = applicationInstanceRepository.findOne(spec).orElseThrow(() ->
                new NotFoundException("No application instance found for " + applicationName + " in " + environmentName)
        );

        Long currentRevision = revisionRepository.currentRevision(ApplicationInstance.class, applicationInstance.getID());

        return getTransformer(currentRevision).apply(applicationInstance);
    }


    @GET
    @Path("application/{application}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findApplicationInstancesByApplication(@PathParam("application") String applicationName,
                                                          @QueryParam("usage") @DefaultValue("true") Boolean showUsage,
                                                          @QueryParam("page") @DefaultValue("0") int page,
                                                          @QueryParam("pr_page") @DefaultValue("100") int pr_page) {
        Application application = validationHelpers.getApplication(applicationName);
        Specification<ApplicationInstance> spec = ApplicationInstanceSpecs.findByApplication(application);
        Page<ApplicationInstance> result = findApplicationInstancesBy(spec, page, pr_page);
        List<ApplicationInstancePayload> payload = createPayload(result, showUsage);

        return PagingBuilder.pagingResponseBuilder(result, uriInfo.getRequestUri()).entity(payload).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationInstancePayload getApplicationInstance(@PathParam("id") Long id) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        Long currentRevision = revisionRepository.currentRevision(ApplicationInstance.class, appInstance.getID());

        return getTransformer(currentRevision).apply(appInstance);
    }

    @GET
    @Path("{id}/appconfig")
    @Produces(MediaType.APPLICATION_XML)
    public Response getAppconfigXml(@PathParam("id") Long id) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        String appconfigXml = appInstance.getAppconfigXml();
        return Response.ok().entity(appconfigXml).build();
    }

    @GET
    @Path("{id}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RevisionPayload<ApplicationInstance>> getRevisions(
            @PathParam("id") Long id) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        List<FasitRevision<ApplicationInstance>> revisions = revisionRepository.getRevisionsFor(ApplicationInstance.class, appInstance.getID());

        List<RevisionPayload<ApplicationInstance>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(uriInfo.getAbsolutePath()))
                .collect(toList());
        return payload;
    }

    @GET
    @Path("{id}/revisions/{revision}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationInstancePayload getApplicationByRevision(@PathParam("id") Long id, @PathParam("revision") Long revision) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        Optional<ApplicationInstance> historic = revisionRepository.getRevisionEntry(ApplicationInstance.class, appInstance.getID(), revision);
        ApplicationInstance old = historic.orElseThrow(() -> new NotFoundException("Revison " + revision + " is not found for application instance " + id));
        return getTransformer(revision).apply(old);
    }

    @GET
    @Path("{id}/revisions/{revision}/appconfig")
    @Produces(MediaType.APPLICATION_XML)
    public Response getAppconfigXmlByRevision(@PathParam("id") Long id, @PathParam("revision") Long revision) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        Optional<ApplicationInstance> historic = revisionRepository.getRevisionEntry(ApplicationInstance.class, appInstance.getID(), revision);
        ApplicationInstance old = historic.orElseThrow(() -> new NotFoundException("Revison " + revision + " is not found for application instance " + id));
        String appconfigXml = old.getAppconfigXml();
        if (appconfigXml == null || appconfigXml.isEmpty()) {
            throw new NotFoundException("No appconfig found for revision " + revision + " on application instance " + id);
        }
        return Response.ok().entity(appconfigXml).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public ApplicationInstancePayload createOrUpdateApplicationInstance(@Valid ApplicationInstancePayload payload) {
        ApplicationInstance existingInstance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment(payload.application, payload.environment);
        if (existingInstance == null) {
            return createApplicationInstance(payload);
            //throw new NotFoundException("Application instance for " + payload.application + " was not found in environment " + payload.environment);
        }
        return updateApplicationInstance(existingInstance.getID(), payload);
    }

    private void verifyThatExposedResourcesAreNotExposedByOtherApps(ApplicationInstancePayload applicationInstancePayload, Optional<Long> existingAppInstanceId) {

        List<String> errorMessages = new ArrayList();

        for (ResourceRefPayload exposedResource : applicationInstancePayload.exposedresources) {
            errorMessages.addAll(applicationInstanceRepository
                    .findAllApplicationInstancesExposingSameResource(exposedResource.id)
                    .stream()
                    .filter(ai -> !existingAppInstanceId.isPresent() || ai.getID() != existingAppInstanceId.get())
                    .map(ai -> "ID: " + ai.getID() + ", name: " + ai.getName() + ", resourceId: " + exposedResource.id + "\n")
                    .collect(toList()));
        }

        if (!errorMessages.isEmpty()) {
            throw new BadRequestException(
                    "Unable to register application instance " + applicationInstancePayload.application + " in " + applicationInstancePayload.environment + " because one or more exposed resources are already exposed by another application instance: \n"
                            + errorMessages + "\nFasit only supports that a resource is exposed by one application instance. ");

        }
    }


    private ApplicationInstancePayload createApplicationInstance(ApplicationInstancePayload payload) {
        if (payload.clusterName == null) {
            throw new BadRequestException("Missing parameter clusterName");
        }

        Environment environment = validationHelpers.findEnvironment(payload.environment);
        Application application = validationHelpers.getApplication(payload.application);
        verifyThatExposedResourcesAreNotExposedByOtherApps(payload, Optional.empty());
        Cluster cluster = findOrCreateCluster(payload, environment);
        ApplicationInstance appInstanceBase = new ApplicationInstance(application, cluster);

        ApplicationInstance appInstance = new Payload2ApplicationInstanceTransformer(appInstanceBase, resourceRepository, revisionRepository).apply(payload);
        ApplicationInstance savedApplicationInstance = applicationInstanceRepository.save(appInstance);
        kafkaProducer.publishDeploymentEvent(savedApplicationInstance, environment);

        return getTransformer().apply(appInstance);

    }

    private Cluster findOrCreateCluster(ApplicationInstancePayload payload, Environment environment) {
        Cluster cluster = environment.findClusterByName(payload.clusterName);
        if (cluster == null) {
            Domain domain = Domain.fromFqdn(payload.domain);
            cluster = new Cluster(payload.clusterName, domain);
            environment.addCluster(cluster);
            Environment savedEnvironment = environmentRepository.save(environment);
            log.debug("Created new cluster {} in environment {}", cluster.getName(), environment.getName());
            return savedEnvironment.findClusterByName(payload.clusterName);
        }
        return cluster;
    }


    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ApplicationInstancePayload updateApplicationInstance(@PathParam("id") Long id, @Valid ApplicationInstancePayload payload) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        AccessChecker.checkAccess(appInstance.getCluster());

        verifyResourcesExists(payload.usedresources);
        verifyResourcesExists(payload.exposedresources);
        verifyThatExposedResourcesAreNotExposedByOtherApps(payload, Optional.of(id));


        ApplicationInstance updated = new Payload2ApplicationInstanceTransformer(appInstance, resourceRepository, revisionRepository).apply(payload);
        lifeCycleSupport.update(updated, payload);

        ApplicationInstance savedApplicationInstance = applicationInstanceRepository.save(updated);
        kafkaProducer.publishDeploymentEvent(savedApplicationInstance, validationHelpers.findEnvironment(payload.environment));

        return getTransformer().apply(updated);
    }


    @DELETE
    @Path("{id}")
    public void deleteApplicationInstance(@PathParam("id") Long id) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        AccessChecker.checkAccess(appInstance.getCluster());

        lifeCycleSupport.delete(appInstance);
        applicationInstanceRepository.delete(appInstance);
    }


    private void verifyResourcesExists(Set<ResourceRefPayload> resourceRefs) {
        for (ResourceRefPayload resourceRef : resourceRefs) {
            if (!resourceRepository.existsById(resourceRef.id)) {
                throw new BadRequestException("Resource with id " + resourceRef.id + " does not exit is Fasit");
            } else {
                if (resourceRef.revision != null && !revisionRepository.getRevisionEntry(Resource.class, resourceRef.id, resourceRef.revision).isPresent()) {
                    throw new BadRequestException("Resource with id " + resourceRef.id + " does not have a revision " + resourceRef.revision);
                }
            }
        }
    }

    private Page<ApplicationInstance> findApplicationInstancesBy(Specification<ApplicationInstance> spec, int page, int pr_page) {
        PageRequest pageRequest = new PageRequest(page, pr_page);

        if (spec == null) {
            return applicationInstanceRepository.findAll(pageRequest);
        } else {
            return applicationInstanceRepository.findAll(spec, pageRequest);
        }
    }

    private List<ApplicationInstancePayload> createPayload(Page<ApplicationInstance> result, boolean showUsage) {
        ApplicationInstance2PayloadTransformer transformer = getTransformer();
        transformer.setShowUsage(showUsage);
        return result.getContent().stream()
                .map(transformer)
                .collect(toList());
    }


    private ApplicationInstance2PayloadTransformer getTransformer(Long currentRevision) {
        return new ApplicationInstance2PayloadTransformer(uriInfo.getBaseUri(), applicationInstanceRepository, currentRevision);
    }

    private ApplicationInstance2PayloadTransformer getTransformer() {
        return new ApplicationInstance2PayloadTransformer(uriInfo.getBaseUri(), applicationInstanceRepository);
    }

    private ApplicationInstance getAppInstanceById(Long id) {
        ApplicationInstance appInstance = applicationInstanceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Application instance with id " + id + " was not found in Fasit")
        );
        return appInstance;
    }

    public static URI instanceUrl(URI baseUri, Long id) {
        return UriBuilder.fromUri(baseUri).path(ApplicationInstanceRest.class).path(ApplicationInstanceRest.class, "getApplicationInstance").build(id);
    }

    public static URI appconfigUri(URI baseUri, Long id) {
        return UriBuilder.fromUri(baseUri).path(ApplicationInstanceRest.class).path(ApplicationInstanceRest.class, "getAppconfigXml").build(id);
    }
}
