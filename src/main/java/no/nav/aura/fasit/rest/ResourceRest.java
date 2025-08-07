package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.FileEntity;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.repository.specs.ResourceSpecs;
import no.nav.aura.fasit.rest.converter.Payload2ResourceTransformer;
import no.nav.aura.fasit.rest.converter.Resource2PayloadTransformer;
import no.nav.aura.fasit.rest.converter.Revision2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;
import no.nav.aura.fasit.rest.model.ResourcePayload;
import no.nav.aura.fasit.rest.model.ResourceTypePayload;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.nav.aura.fasit.rest.helpers.PagingBuilder.pagingResponseBuilder;
import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;

@Component

@Path("api/v2/resources")
public class ResourceRest extends AbstractResourceRest {


    @Context
    private UriInfo uriInfo;

    private final static Logger log = LoggerFactory.getLogger(ResourceRest.class);

    @Inject
    public ResourceRest(
            FasitRepository repo,
            ResourceRepository resourceRepository,
            ApplicationInstanceRepository applicationInstanceRepository,
            ValidationHelpers validationHelpers,
            RevisionRepository revisionRepository,
            LifeCycleSupport lifeCycleSupport) {

        super(repo, resourceRepository, applicationInstanceRepository, validationHelpers, lifeCycleSupport, revisionRepository);
    }

    /**
     * find resource by id
     *
     * @responseMessage 404 Resource not found with this id
     */
    @GET
    @Path("{resourceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResourcePayload getResource(@PathParam("resourceId") String resourceId) {
        Resource resource = getResourceById(resourceId);
        Long currentRevision = revisionRepository.currentRevision(Resource.class, resource.getID());
        Resource2PayloadTransformer transformer = createTransformer(currentRevision);
        transformer.setShowUsage(true);
        return transformer.apply(resource);
    }

    /**
     * show revision history for a given resource
     *
     * @responseMessage 404 Resource not found with this id
     */
    @GET
    @Path("{resourceId}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RevisionPayload<Resource>> Revisions(
            @PathParam("resourceId") String resourceId) {

        Resource resource = getResourceById(resourceId);
        List<FasitRevision<Resource>> revisions = revisionRepository.getRevisionsFor(Resource.class, resource.getID());

        List<RevisionPayload<Resource>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(uriInfo.getAbsolutePath()))
                .collect(toList());
        return payload;
    }

    /**
     * show revision info for a given resource and a given revision number
     *
     * @responseMessage 404 Node with resourceId not found
     * @responseMessage 404 Revision number not found
     */
    @GET
    @Path("{resourceId}/revisions/{revision}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResourcePayload getResourceByRevision(@PathParam("resourceId") String resourceId, @PathParam("revision") Long revision) {
        Resource resource = getResourceById(resourceId);
        Optional<Resource> historicResource = revisionRepository.getRevisionEntry(Resource.class, resource.getID(), revision);
        Resource oldResource = historicResource.orElseThrow(() -> new NotFoundException("Revison " + revision + " is not found for resource " + resourceId));
        Resource2PayloadTransformer transformer = createTransformer(revision);
        transformer.setShowUsage(true);
        return transformer.apply(oldResource);
    }

    /**
     * Download a file for a given resource
     *
     * @responseMessage 404 Resource not found with this id
     * @responseMessage 404 file not found with this name for the current resource
     */
    @GET
    @Path("/{resourceId}/file/{filename}")
    @Produces("application/octet-stream")
    public Response getFile(@PathParam("resourceId") String resourceId, @PathParam("filename") String filename) {
        Resource resource = getResourceById(resourceId);
        FileEntity fileEntity = resource.getFiles().get(filename);

        if (fileEntity == null) {
            throw new NotFoundException("No file found with name " + filename + " for resource " + resourceId);
        }

        Response.ResponseBuilder builder = Response.ok(new ByteArrayInputStream(fileEntity.getFileData()));
        builder.header("Content-Disposition", "attachment; filename=\"" + fileEntity.getName() + "\"");
        return builder.build();
    }

    /**
     * Find resources by query params.
     * Usage query param is used for showing which application instances are using this resource and which application is exposing it.
     * Call to this service with no params gives every resource in a paged response.
     * Setting pr_page param to a high number, will result in a high respone time
     * Query params can be used in any permutation
     *
     * @responseType java.util.List<no.nav.aura.fasit.rest.model.ResourcePayload>
     * @responseMessage 400 Environment name in query param does not exist
     * @responseMessage 400 Application name in query param does not exist
     * @responseMessage 400 Zone query param has to be combined with environment or environment class to make sense as this is translated to a domain name
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findResources(
            @QueryParam("alias") String alias,
            @QueryParam("type") ResourceType type,
            @QueryParam("environmentclass") EnvironmentClass environmentClass,
            @QueryParam("environment") String environmentName,
            @QueryParam("zone") Zone zone,
            @QueryParam("application") String applicationName,
            @QueryParam("status") LifeCycleStatus lifeCycleStatus,
            @QueryParam("usage") @DefaultValue("false") Boolean showUsage,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pr_page") @DefaultValue("100") int pr_page) {
        Optional<Environment> environment = validationHelpers.getOptionalEnvironment(environmentName);
        Optional<Application> application = validationHelpers.getOptionalApplication(applicationName);
        Optional<Domain> domain = validationHelpers.domainFromZone(environmentClass, environment, zone);

        Specification<Resource> spec = ResourceSpecs.findByLikeAlias(alias, type, environmentClass, environment, domain, application, lifeCycleStatus);
        PageRequest pageRequest = new PageRequest(page, pr_page);

        Page<Resource> resources;
        if (spec != null) {
            resources = resourceRepository.findAll(spec, pageRequest);
        } else {
            resources = resourceRepository.findAll(pageRequest);
        }

        Resource2PayloadTransformer transformer = createTransformer();
        transformer.setShowUsage(showUsage);

        List<ResourcePayload> resourcesPayload = resources.getContent().stream().map(transformer).collect(toList());

        return pagingResponseBuilder(resources, uriInfo.getRequestUri()).entity(resourcesPayload).build();
    }


    /**
     * Shows supported resource types and the attributes for each type
     */
    @GET
    @Path("types")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ResourceTypePayload> getResoureTypes() {
        List<ResourceTypePayload> resoureTypes = new ArrayList<>();

        for (ResourceType value : ResourceType.values()) {
            resoureTypes.add(new ResourceTypePayload(value));
        }
        return resoureTypes;
    }

    /**
     * Shows supported attributes for a specific type
     *
     * @responeMessage 400 Resource type does not exist
     */
    @GET
    @Path("types/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResourceTypePayload getResoureTypes(@PathParam("type") ResourceType type) {
        return new ResourceTypePayload(type);
    }


    /**
     * TODO crate doc. Sometin bout ResourceType and properties to use. Link to resource type api
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createResource(@Valid ResourcePayload payload) {
        Resource resource = new Payload2ResourceTransformer(validationHelpers, null).apply(payload);
        checkAccess(resource);
        checkDuplicate(resource);

        Resource saved = resourceRepository.save(resource);
        URI resourceUrl = uriInfo.getAbsolutePathBuilder().path(valueOf(saved.getID())).build();
        return Response.created(resourceUrl).build();
    }

    @PUT
    @Path("{resourceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public ResourcePayload updateResource(@PathParam("resourceId") String resourceId, @Valid ResourcePayload payload) {
        Resource oldResource = getResourceById(resourceId);
        checkAccess(oldResource);

        if (oldResource.getType() != payload.type) {
            throw new BadRequestException("Change of resource type is not allowed. Delete this resource and create a new one");
        }

        Resource updatedResource = new Payload2ResourceTransformer(validationHelpers, oldResource).apply(payload);

        lifeCycleSupport.update(oldResource, payload);
        resourceRepository.save(updatedResource);

        URI resourceUrl = uriInfo.getAbsolutePathBuilder().path(valueOf(updatedResource.getID())).build();
        return new Resource2PayloadTransformer(repo, applicationInstanceRepository, resourceUrl).apply(updatedResource);
    }

    /**
     * Delete resource by ID
     *
     * @responseMessage 404 Resource id does not exist
     */
    @DELETE
    @Path("{resourceId}")
    @Transactional
    public void deleteResource(@PathParam("resourceId") String resourceId) {
        Resource resourceToDelete = getResourceById(resourceId);
        checkAccess(resourceToDelete);
        lifeCycleSupport.delete(resourceToDelete);

        List<ApplicationInstance> applicationInstances = applicationInstanceRepository.findAllApplicationInstancesExposingSameResource(resourceToDelete.getID());

        boolean deleted = false;

        for (ApplicationInstance applicationInstance : applicationInstances) {
            log.debug("Removing exposed resource {} from application instance {} ", resourceToDelete, applicationInstance);
            Set<ExposedServiceReference> exposedServices = applicationInstance.getExposedServices();

            Optional<ExposedServiceReference> exposedService = exposedServices
                    .stream()
                    .filter(exposedServiceReference -> exposedServiceReference.getResource().getID().equals(resourceToDelete.getID()))
                    .findFirst();

            if (exposedService.isPresent()) {
                exposedServices.remove(exposedService.get());
                repo.delete(exposedService.get());
                deleted = true;
                //return;
            }
        }

        if (!deleted) {
            resourceRepository.delete(resourceToDelete);
        }

    }


    private void checkDuplicate(Resource resource) {
        Scope scope = resource.getScope();
        Optional<Environment> environment = validationHelpers.getOptionalEnvironment(scope.getEnvironmentName());
        Optional<Domain> domain = optional(scope.getDomain());
        Optional<Application> application = optional(scope.getApplication());

        Specification<Resource> spec = ResourceSpecs.findByExcactAlias(
                resource.getAlias(), resource.getType(), scope.getEnvClass(), environment, domain, application);
        List<Resource> existingResources = resourceRepository.findAll(spec);
        List<Resource> filteredDuplicates = filterDuplicateCandidates(environment, existingResources);


        if (filteredDuplicates.size() > 0) {
            throw new BadRequestException(
                    format("Duplicate resource: \n" +
                            filteredDuplicates
                                    .stream()
                                    .map(r -> r.getID() + " " + r.getScope().getDisplayString())
                                    .collect(joining("\n"))));
        }

    }

    // If no environment exist in resource scope, we want to filter out all duplicates that are scoped to specific environments.
    // It is allowed to have a generic resource scoped to just an env class and multiple identical resources scoped to specific environments.
    // This is a step towards reducing the number of paralell test environments
    private List<Resource> filterDuplicateCandidates(Optional<Environment> environment, List<Resource> existingResources) {
        if(!environment.isPresent()) {
            return existingResources
                    .stream()
                    .filter(r -> r.getScope().getEnvironmentName() == null)
                    .collect(toList());
        }
        else {
            return existingResources;
        }
    }

    private Resource getResourceById(String resourceId) {
        Resource resourceById = resourceRepository.findById(parseLong(resourceId)).orElseThrow(() ->
                new NotFoundException("No resource found with id " + resourceId)
        );
        return resourceById;
    }
}
