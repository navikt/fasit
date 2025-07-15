package no.nav.aura.fasit.rest;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
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

@RestController
@RequestMapping("/api/v2/applicationinstances")
public class ApplicationInstanceRest {
    @Inject
    private ApplicationInstanceRepository applicationInstanceRepository;
    @Inject
    private ResourceRepository resourceRepository;
    @Autowired
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

    public ApplicationInstanceRest() {
    }

    public ApplicationInstanceRest(ApplicationInstanceRepository applicationInstanceRepository, RevisionRepository revisionRepository, ResourceRepository resourceRepository, ResourceRest resourceRest) {
        this.applicationInstanceRepository = applicationInstanceRepository;
        this.revisionRepository = revisionRepository;
        this.resourceRepository = resourceRepository;
        this.resourceRest = resourceRest;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> findApplicationInstances(
    		@RequestParam(name = "environment", required = false) String environmentName,
    		@RequestParam(name = "environmentclass", required = false) EnvironmentClass environmentClass,
    		@RequestParam(name = "application", required = false) String applicationName,
    		@RequestParam(name = "status", required = false) LifeCycleStatus lifeCycleStatus,
    		@RequestParam(name = "usage", defaultValue = "true") Boolean showUsage,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name = "pr_page", defaultValue = "100") int pr_page) {
        // @TODO cleanup
//        LifeCycleStatus lifeCycleStatus = null;
//        if (lifeCycleStatusStr != null) {
//            lifeCycleStatus = LifeCycleStatus.valueOf(lifeCycleStatusStr.toUpperCase());
//        }
        Specification<ApplicationInstance> spec = ApplicationInstanceSpecs.find(environmentName, environmentClass, applicationName, lifeCycleStatus);

        Page<ApplicationInstance> result = findApplicationInstancesBy(spec, page, pr_page);
        List<ApplicationInstancePayload> payload = createPayload(result, showUsage);
        return PagingBuilder.pagingResponseBuilder(result, getBaseUri()).body(payload);
    }


    @GetMapping(path = "/environment/{environment}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> findApplicationInstancesByEnvironment(
    		@PathVariable("environment") String environmentName,
    		@RequestParam(name = "usage", defaultValue = "true") Boolean showUsage,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name = "pr_page", defaultValue = "100") int pr_page) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Specification<ApplicationInstance> spec = ApplicationInstanceSpecs.findByEnvironment(environment);
        Page<ApplicationInstance> result = findApplicationInstancesBy(spec, page, pr_page);
        List<ApplicationInstancePayload> payload = createPayload(result, showUsage);

        return PagingBuilder.pagingResponseBuilder(result, getBaseUri()).body(payload);
    }

    @GetMapping(path = "/environment/{environment}/application/{application}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationInstancePayload findAppInstanceByEnvAndApp(
    		@PathVariable("environment") String environmentName,
    		@PathVariable("application") String applicationName) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Application application = validationHelpers.getApplication(applicationName);

        Specification<ApplicationInstance> spec = ApplicationInstanceSpecs.findByEnvironmentAndApplication(environment, application);
        ApplicationInstance applicationInstance = applicationInstanceRepository.findOne(spec).orElseThrow(() ->
	        new ResponseStatusException(HttpStatus.NOT_FOUND, "No application instance found for " + applicationName + " in " + environmentName)
	);

        Long currentRevision = revisionRepository.currentRevision(ApplicationInstance.class, applicationInstance.getID());

        return getTransformer(currentRevision).apply(applicationInstance);
    }


    @GetMapping(path = "/application/{application}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> findApplicationInstancesByApplication(@PathVariable("application") String applicationName,
    		@RequestParam(name = "usage", defaultValue = "true") Boolean showUsage,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name = "pr_page", defaultValue = "100") int pr_page) {
        Application application = validationHelpers.getApplication(applicationName);
        Specification<ApplicationInstance> spec = ApplicationInstanceSpecs.findByApplication(application);
        Page<ApplicationInstance> result = findApplicationInstancesBy(spec, page, pr_page);
        List<ApplicationInstancePayload> payload = createPayload(result, showUsage);

        return PagingBuilder.pagingResponseBuilder(result, getBaseUri()).body(payload);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationInstancePayload getApplicationInstance(@PathVariable("id") Long id) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        Long currentRevision = revisionRepository.currentRevision(ApplicationInstance.class, appInstance.getID());

        return getTransformer(currentRevision).apply(appInstance);
    }

    @GetMapping(path = "/{id}/appconfig", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> getAppconfigXml(@PathVariable("id") Long id) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        String appconfigXml = appInstance.getAppconfigXml();
        return ResponseEntity.ok(appconfigXml);
    }

    @GetMapping(path = "/{id}/revisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RevisionPayload<ApplicationInstance>> getRevisions(
            @PathVariable("id") Long id) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        List<FasitRevision<ApplicationInstance>> revisions = revisionRepository.getRevisionsFor(ApplicationInstance.class, appInstance.getID());

        List<RevisionPayload<ApplicationInstance>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(getBaseUri()))
                .collect(toList());
        return payload;
    }

    @GetMapping(path = "/{id}/revisions/{revision}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationInstancePayload getApplicationByRevision(@PathVariable("id") Long id, @PathVariable("revision") Long revision) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        Optional<ApplicationInstance> historic = revisionRepository.getRevisionEntry(ApplicationInstance.class, appInstance.getID(), revision);
        ApplicationInstance old = historic.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revison " + revision + " is not found for application instance " + id));
        return getTransformer(revision).apply(old);
    }

    @GetMapping(path = "/{id}/revisions/{revision}/appconfig", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getAppconfigXmlByRevision(@PathVariable("id") Long id, @PathVariable("revision") Long revision) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        Optional<ApplicationInstance> historic = revisionRepository.getRevisionEntry(ApplicationInstance.class, appInstance.getID(), revision);
        ApplicationInstance old = historic.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revison " + revision + " is not found for application instance " + id));
        String appconfigXml = old.getAppconfigXml();
        if (appconfigXml == null || appconfigXml.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No appconfig found for revision " + revision + " on application instance " + id);
        }
        return ResponseEntity.ok(appconfigXml);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApplicationInstancePayload createOrUpdateApplicationInstance(
    		@Valid @RequestBody ApplicationInstancePayload payload) {
        log.info("Create or update application instance {} {}", payload.application, payload.environment);
        ApplicationInstance existingInstance = applicationInstanceRepository.findInstanceOfApplicationInEnvironment(payload.application, payload.environment);
        if (existingInstance == null) {
            log.info("Application not found. Creating {}", payload.application);
            return createApplicationInstance(payload);
            //throw new NotFoundException("Application instance for " + payload.application + " was not found in environment " + payload.environment);
        }
        log.info("Updating application {} in environment {}", payload.application, payload.environment);
        return updateApplicationInstance(existingInstance.getID(), payload);
    }

    private void verifyThatExposedResourcesAreNotExposedByOtherApps(ApplicationInstancePayload applicationInstancePayload, Optional<Long> existingAppInstanceId) {

        List<String> errorMessages = new ArrayList<>();

        for (ResourceRefPayload exposedResource : applicationInstancePayload.exposedresources) {
            errorMessages.addAll(applicationInstanceRepository
                    .findAllApplicationInstancesExposingSameResource(exposedResource.id)
                    .stream()
                    .filter(ai -> !existingAppInstanceId.isPresent() || ai.getID() != existingAppInstanceId.get())
                    .map(ai -> "ID: " + ai.getID() + ", name: " + ai.getName() + ", resourceId: " + exposedResource.id + "\n")
                    .collect(toList()));
        }

        if (!errorMessages.isEmpty()) {
        	throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Unable to register application instance " + applicationInstancePayload.application + " in " + applicationInstancePayload.environment + 
					" because one or more exposed resources are already exposed by another application instance: \n" +
					errorMessages + "\nFasit only supports that a resource is exposed by one application instance. ");

        }
    }


    private ApplicationInstancePayload createApplicationInstance(ApplicationInstancePayload payload) {
        if (payload.clusterName == null) {
        	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing parameter clusterName");
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


    @PutMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationInstancePayload updateApplicationInstance(@PathVariable("id") Long id, @Valid @RequestBody ApplicationInstancePayload payload) {
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


    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteApplicationInstance(@PathVariable("id") Long id) {
        ApplicationInstance appInstance = getAppInstanceById(id);
        AccessChecker.checkAccess(appInstance.getCluster());

        lifeCycleSupport.delete(appInstance);
        //vera.notifyVeraOfUndeployment(applicationInstance.getApplication().getName(), environment.getName(), EntityCommenter.getOnBehalfUserOrRealUser(applicationInstance));
        applicationInstanceRepository.delete(appInstance);
        return ResponseEntity.noContent().build();
    }


    private void verifyResourcesExists(Set<ResourceRefPayload> resourceRefs) {
        for (ResourceRefPayload resourceRef : resourceRefs) {
            if (!resourceRepository.existsById(resourceRef.id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource with id " + resourceRef.id + " does not exit is Fasit");
            } else {
                if (resourceRef.revision != null && !revisionRepository.getRevisionEntry(Resource.class, resourceRef.id, resourceRef.revision).isPresent()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource with id " + resourceRef.id + " does not have a revision " + resourceRef.revision);
                }
            }
        }
    }

    private Page<ApplicationInstance> findApplicationInstancesBy(Specification<ApplicationInstance> spec, int page, int pr_page) {
        PageRequest pageRequest = PageRequest.of(page, pr_page);

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
        return new ApplicationInstance2PayloadTransformer(getBaseUri(), applicationInstanceRepository, currentRevision);
    }

    private ApplicationInstance2PayloadTransformer getTransformer() {
        return new ApplicationInstance2PayloadTransformer(getBaseUri(), applicationInstanceRepository);
    }
    
    private URI getBaseUri() {
		return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
	}

    private ApplicationInstance getAppInstanceById(Long id) {
        ApplicationInstance appInstance = applicationInstanceRepository.findById(id).orElseThrow(() ->
        		
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Application instance with id " + id + " was not found in Fasit")
        );
        return appInstance;
    }

    public static URI instanceUrl(URI baseUri, Long id) {
        return ServletUriComponentsBuilder.fromUri(baseUri)
                .path("/applicationinstances/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    public static URI appconfigUri(URI baseUri, Long id) {
        return ServletUriComponentsBuilder.fromUri(baseUri)
                .path("/applicationinstances/{id}/appconfig")
                .buildAndExpand(id)
                .toUri();
    }

}
