package no.nav.aura.fasit.rest;

import static no.nav.aura.fasit.rest.helpers.PagingBuilder.pagingResponseBuilder;
import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

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

@RestController
@RequestMapping(path = "/api/v2/environments")
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

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> findEnvironments(
            @RequestParam(name = "environmentclass", required = false) EnvironmentClass environmentClass,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "status", required = false) LifeCycleStatus lifeCycleStatus,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pr_page", defaultValue = "100") int pr_page) {

        Specification<Environment> spec = EnvironmentSpecs.find(environmentClass, name, lifeCycleStatus);

        PageRequest pageRequest = PageRequest.of(page, pr_page);
        Page<Environment> environments;

        if (spec != null) {
            environments = environmentRepository.findAll(spec, pageRequest);
        } else {
            environments = environmentRepository.findAll(pageRequest);
        }
        URI baseUri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

        List<EnvironmentPayload> result = environments.getContent().stream()
                .map(new Environment2PayloadTransformer(baseUri))
                .collect(Collectors.toList());

        return pagingResponseBuilder(environments, baseUri).body(result);
    }

    @GetMapping(path = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public EnvironmentPayload getEnvironment(@PathVariable("name") String environmentName) {
        Environment environment = validationHelpers.findEnvironment(environmentName);
        Long currentRevision = revisionRepository.currentRevision(Environment.class, environment.getID());
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

        return new Environment2PayloadTransformer(baseUri, currentRevision).apply(environment);
    }

    @GetMapping(path = "/{name}/revisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RevisionPayload<Environment>> getRevisions(
            @PathVariable("name") String environmentName) {
        Environment environment = validationHelpers.findEnvironment(environmentName);
        List<FasitRevision<Environment>> revisions = revisionRepository.getRevisionsFor(Environment.class, environment.getID());

        URI baseUri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

        List<RevisionPayload<Environment>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(baseUri))
                .collect(Collectors.toList());
        return payload;
    }

    @GetMapping(path = "/{name}/revisions/{revision}", produces = MediaType.APPLICATION_JSON_VALUE)
    public EnvironmentPayload getEnvironmentByRevision(@PathVariable(name = "name") String environmentName, @PathVariable(name = "revision") Long revision) {
        Environment environment = validationHelpers.findEnvironment(environmentName);
        Optional<Environment> historic = revisionRepository.getRevisionEntry(Environment.class, environment.getID(), revision);
        Environment old = historic.orElseThrow(() -> 
        	new ResponseStatusException(HttpStatus.NOT_FOUND, "Revision " + revision + " is not found for environment " + environmentName)
        	);
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

        return new Environment2PayloadTransformer(baseUri, revision).apply(old);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> createEnvironment(@Valid @RequestBody EnvironmentPayload payload) {
        Environment existingEnvironment = environmentRepository.findByNameIgnoreCase(payload.name);
        if (existingEnvironment != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Environment with name " + payload.name + " already exists");
        }

        Environment environment = new Payload2EnvironmentTransformer().apply(payload);
        checkAccess(environment);
        log.info("Creating new environment {}", environment.getName());
        environmentRepository.save(environment);
        URI environmentUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v2/environments/{name}")
                .buildAndExpand(environment.getName())
                .toUri();
        return ResponseEntity.created(environmentUri).build();
    }

    @PutMapping(path = "/{name}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public EnvironmentPayload updateEnvironment(@PathVariable(name = "name") String environmentName, @Valid @RequestBody EnvironmentPayload payload) {
        Environment existingEnvironment = validationHelpers.findEnvironment(environmentName);
        checkAccess(existingEnvironment);
        if (!existingEnvironment.getEnvClass().equals(payload.environmentClass)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "It is not possible to change environmentclass on an environment. Existing: " + existingEnvironment.getEnvClass() + ", new: " + payload.environmentClass);
        }
        Environment updatedEnvironment = new Payload2EnvironmentTransformer(existingEnvironment).apply(payload);
        lifeCycleSupport.update(updatedEnvironment, payload);
        log.info("Updating environment {}", environmentName);
        environmentRepository.save(updatedEnvironment);
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

        return new Environment2PayloadTransformer(baseUri).apply(updatedEnvironment);
    }

    @DeleteMapping(path = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Void> delete(
    		@PathVariable(name = "name") String environmentName) {
        Environment existingEnvironment = validationHelpers.findEnvironment(environmentName);
        checkAccess(existingEnvironment);
        //TODO check if environment is empty?

        log.info("Updating environment {}", environmentName);
        lifeCycleSupport.delete(existingEnvironment);
        environmentRepository.delete(existingEnvironment);
        return ResponseEntity.noContent().build();
    }
}
