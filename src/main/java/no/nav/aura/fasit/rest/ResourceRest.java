package no.nav.aura.fasit.rest;

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.nav.aura.fasit.rest.helpers.PagingBuilder.pagingResponseBuilder;
import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.ExposedServiceReference;
import no.nav.aura.envconfig.model.infrastructure.Zone;
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

@RestController
@RequestMapping(path = "/api/v2/resources")
public class ResourceRest extends AbstractResourceRest {

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
    @GetMapping(path = "/{resourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResourcePayload getResource(@PathVariable(name = "resourceId") String resourceId) {
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
    @GetMapping(path = "/{resourceId}/revisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RevisionPayload<Resource>> Revisions(
            @PathVariable(name = "resourceId") String resourceId) {

        Resource resource = getResourceById(resourceId);
        List<FasitRevision<Resource>> revisions = revisionRepository.getRevisionsFor(Resource.class, resource.getID());

        URI absPath = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

        List<RevisionPayload<Resource>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(absPath))
                .collect(toList());
        return payload;
    }

    /**
     * show revision info for a given resource and a given revision number
     *
     * @responseMessage 404 Node with resourceId not found
     * @responseMessage 404 Revision number not found
     */
    @GetMapping(path = "/{resourceId}/revisions/{revision}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResourcePayload getResourceByRevision(@PathVariable(name = "resourceId") String resourceId, @PathVariable(name = "revision") Long revision) {
        Resource resource = getResourceById(resourceId);
        Optional<Resource> historicResource = revisionRepository.getRevisionEntry(Resource.class, resource.getID(), revision);
        Resource oldResource = historicResource.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revison " + revision + " is not found for resource " + resourceId));
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
    @GetMapping(path = "/{resourceId}/file/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable(name ="resourceId") String resourceId, @PathVariable(name = "filename") String filename) {
        Resource resource = getResourceById(resourceId);
        FileEntity fileEntity = resource.getFiles().get(filename);

        if (fileEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No file found with name " + filename + " for resource " + resourceId);
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getName() + "\"")
                .body(fileEntity.getFileData());
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
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> findResources(
            @RequestParam(name = "alias", required = false) String alias,
            @RequestParam(name = "type", required = false) ResourceType type,
            @RequestParam(name = "environmentclass", required = false) EnvironmentClass environmentClass,
            @RequestParam(name = "environment", required = false) String environmentName,
            @RequestParam(name = "zone", required = false) String zoneStr,
            @RequestParam(name = "application", required = false) String applicationName,
            @RequestParam(name = "status", required = false) String lifeCycleStatusStr,
            @RequestParam(name = "usage", defaultValue = "false") Boolean showUsage,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pr_page", defaultValue = "100") int pr_page) {
        LifeCycleStatus lifeCycleStatus = null;
        if (lifeCycleStatusStr != null) {
            lifeCycleStatus = LifeCycleStatus.valueOf(lifeCycleStatusStr.toUpperCase());
        }
        Zone zone = null;
        if (zoneStr != null) {
        	zone = Zone.valueOf(zoneStr.toUpperCase());
        }
        Optional<Environment> environment = validationHelpers.getOptionalEnvironment(environmentName);
        Optional<Application> application = validationHelpers.getOptionalApplication(applicationName);
        Optional<Domain> domain = validationHelpers.domainFromZone(environmentClass, environment, zone);

        Specification<Resource> spec = ResourceSpecs.findByLikeAlias(alias, type, environmentClass, environment, domain, application, lifeCycleStatus);
        PageRequest pageRequest = PageRequest.of(page, pr_page);

        Page<Resource> resources;
        if (spec != null) {
            resources = resourceRepository.findAll(spec, pageRequest);
        } else {
            resources = resourceRepository.findAll(pageRequest);
        }

        Resource2PayloadTransformer transformer = createTransformer();
        transformer.setShowUsage(showUsage);

        List<ResourcePayload> resourcesPayload = resources.getContent().stream().map(transformer).collect(toList());

        URI requestUri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return pagingResponseBuilder(resources, requestUri).body(resourcesPayload);
    }


    /**
     * Shows supported resource types and the attributes for each type
     */
    @GetMapping(path = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @GetMapping(path = "/types/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResourceTypePayload getResoureTypes(@PathVariable(name = "type") ResourceType type) {
        return new ResourceTypePayload(type);
    }


    /**
     * TODO crate doc. Sometin bout ResourceType and properties to use. Link to resource type api
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Void> createResource(@Valid @RequestBody ResourcePayload payload) {
        Resource resource = new Payload2ResourceTransformer(validationHelpers, null).apply(payload);
        checkAccess(resource);
        checkDuplicate(resource);

        Resource saved = resourceRepository.save(resource);
        URI resourceUrl = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(valueOf(saved.getID()))
                .toUri();
        return ResponseEntity.created(resourceUrl).build();
    }

    @PutMapping(path = "/{resourceId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResourcePayload updateResource(@PathVariable(name = "resourceId") String resourceId, @Valid @RequestBody ResourcePayload payload) {
        Resource oldResource = getResourceById(resourceId);
        checkAccess(oldResource);

        if (oldResource.getType() != payload.type) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Change of resource type is not allowed. Delete this resource and create a new one");
        }

        Resource updatedResource = new Payload2ResourceTransformer(validationHelpers, oldResource).apply(payload);

        lifeCycleSupport.update(oldResource, payload);
        resourceRepository.save(updatedResource);

        URI resourceUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return new Resource2PayloadTransformer(repo, applicationInstanceRepository, resourceUrl).apply(updatedResource);
    }

    /**
     * Delete resource by ID
     *
     * @responseMessage 404 Resource id does not exist
     */
    @DeleteMapping(path = "/{resourceId}")
    @Transactional
    public ResponseEntity<Void> deleteResource(@PathVariable(name = "resourceId") String resourceId) {
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
        return ResponseEntity.noContent().build();
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
        	throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No resource found with id " + resourceId)
        );
        return resourceById;
    }
}
