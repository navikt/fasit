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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.aura.fasit.rest.helpers.PagingBuilder.pagingResponseBuilder;
import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;
import static no.nav.aura.fasit.rest.security.AccessChecker.checkSuperuserAccess;

@RestController
@RequestMapping(path = "/api/v2/applications")
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

    public ApplicationRest() {
    }

    /**
     * Find a list of applications
     *
     * @param name filter application name containing
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> findApplications(
    		@RequestParam(name = "name", required = false) String name,
    		@RequestParam(name = "status", required = false) LifeCycleStatus lifeCycleStatus,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name= "pr_page", defaultValue = "100") int pr_page) {

        Specification<Application> spec = ApplicationSpecs.find(name, lifeCycleStatus);
        PageRequest pageRequest = PageRequest.of(page, pr_page);

        Page<Application> applications;

        if (spec != null){
            applications = applicationRepository.findAll(spec, pageRequest);
        } else {
            applications = applicationRepository.findAll(pageRequest);
        }

//        List<ApplicationPayload> result = applications.getContent().stream()
//                .map(new Application2PayloadTransformer(uriInfo.getBaseUri()))
//                .collect(Collectors.toList());
//
//        return pagingResponseBuilder(applications, uriInfo.getRequestUri()).entity(result).build();

        URI baseUri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        List<ApplicationPayload> result = applications.getContent().stream()
                .map(new Application2PayloadTransformer(baseUri))
                .collect(Collectors.toList());

        return pagingResponseBuilder(applications, baseUri).body(result);
    }

    @GetMapping(path = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApplicationPayload getApplication(@PathVariable(name = "name") String applicationName) {
        Application application = validationHelpers.findApplication(applicationName);
        Long currentRevision = revisionRepository.currentRevision(Application.class, application.getID());
        
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

        return new Application2PayloadTransformer(baseUri, currentRevision).apply(application);
    }

    @GetMapping(path = "/{name}/revisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RevisionPayload<Application>> getRevisions(
            @PathVariable(name= "name") String applicationName) {
        Application application = validationHelpers.findApplication(applicationName);
        List<FasitRevision<Application>> revisions = revisionRepository.getRevisionsFor(Application.class, application.getID());
        
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        List<RevisionPayload<Application>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(baseUri))
                .collect(Collectors.toList());
        return payload;
    }

    @GetMapping(path = "/{name}/revisions/{revision}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationPayload getApplicationByRevision(
    		@PathVariable(name = "name") String applicationName, 
    		@PathVariable(name = "revision") Long revision) {
        Application application = validationHelpers.findApplication(applicationName);
        Optional<Application> historic = revisionRepository.getRevisionEntry(Application.class, application.getID(), revision);
        Application old = historic.orElseThrow(() -> 
        	new ResponseStatusException(HttpStatus.NOT_FOUND, "Revison " + revision + " is not found for application " + applicationName)
        	);
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return new Application2PayloadTransformer(baseUri, revision).apply(old);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Void> createApplication(@Valid @RequestBody ApplicationPayload payload) {
        Application existing = applicationRepository.findByNameIgnoreCase(payload.name);
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application with name " + payload.name + " already exists");
        }
        Application application = new Payload2ApplicationTransformer().apply(payload);
        checkSuperuserAccess();

        log.info("Creating new application {}", application.getName());
        applicationRepository.save(application);
        URI locationUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v2/applications/{name}")
                .buildAndExpand(application.getName())
                .toUri();
        
        return ResponseEntity.created(locationUri).build();
        
    }

    @PutMapping(path = "/{name}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApplicationPayload updateApplication(@PathVariable("name") String applicationName, @Valid @RequestBody ApplicationPayload payload) {
        Application existing = validationHelpers.findApplication(applicationName);
        if (!existing.getName().equals(payload.name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "It is not possible to change name of an application. Delete it and create a new one. Existing :" + existing.getName() + " new:" + payload.name);
        }
        checkAccess(existing);
        validatePortConflicts(existing, payload);
        Application application = new Payload2ApplicationTransformer(existing).apply(payload);
        lifeCycleSupport.update(existing, payload);
        log.info("Updating application {}", application.getName());
        applicationRepository.save(application);
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return new Application2PayloadTransformer(baseUri).apply(application);
    }


    @DeleteMapping(path = "/{name}")
    @Transactional
    public ResponseEntity<Void> deleteApplication(@PathVariable("name") String applicationName) {
        Application application = validationHelpers.findApplication(applicationName);
        checkAccess(application);
        validateNoDeployedInstancesOnDelete(application);
        lifeCycleSupport.delete(application);
        log.info("Deleting application {}", application.getName());
        applicationRepository.delete(application);
        return ResponseEntity.noContent().build();
        
    }

    private void validateNoDeployedInstancesOnDelete(Application application) {
        List<ApplicationInstance> instances = applicationInstanceRepository.findAll(ApplicationInstanceSpecs.findByApplicationName(application.getName()));
        long deployedCount = instances.stream()
                .filter(ai -> ai.isDeployed())
                .count();

        if (deployedCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application " + application.getName() + " can not be deleted because it is deployed to " + deployedCount + " environment(s)");
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

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conflicting portoffset with application " + conflicting.get().getName() + " mapped to the same cluster as this application. You can't use ports " + usedPorts);
            }

        }

    }
}
