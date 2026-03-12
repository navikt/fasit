package no.nav.aura.fasit.rest;

import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
import org.springframework.web.util.UriBuilder;

import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.ClusterRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.rest.converter.Cluster2PayloadTransformer;
import no.nav.aura.fasit.rest.converter.Payload2ClusterTransformer;
import no.nav.aura.fasit.rest.converter.Revision2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;
import no.nav.aura.fasit.rest.model.ClusterPayload;
import no.nav.aura.fasit.rest.model.RevisionPayload;

@RestController
@RequestMapping(path = "/api/v2/environments/{environment}/clusters")
public class ClusterRest {

    @Inject
    private EnvironmentRepository environmentRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private ApplicationRepository applicationRepository;
    @Inject
    private LifeCycleSupport lifeCycleSupport;
    @Inject
    private RevisionRepository revisionRepository;

    @Inject
    private ValidationHelpers validationHelpers;

    private final static Logger log = LoggerFactory.getLogger(ClusterRest.class);


    public static URI clusterUrl(URI baseUri, Environment environment, Cluster cluster) {
        return ServletUriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/environments/{envName}/clusters/{clusterName}")
                .buildAndExpand(environment.getName(), cluster.getName())
                .toUri();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ClusterPayload> findClusters(
            @PathVariable(name = "environment") String environmentName,
            @RequestParam(name = "status", required = false) LifeCycleStatus lifeCycleStatus) {

        Environment environment = validationHelpers.getEnvironment(environmentName);
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

        return environment.getClusters().stream()
                .filter(cluster -> lifeCycleStatus == null || lifeCycleStatus.equals(cluster.getLifeCycleStatus()))
                .sorted(Comparator.comparing(Cluster::getName))
                .map(new Cluster2PayloadTransformer(baseUri, environment))
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/{clustername}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ClusterPayload getCluster(@PathVariable(name = "environment") String environmentName, @PathVariable(name = "clustername") String clustername) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster cluster = findCluster(environment, clustername);
        Long currentRevision = revisionRepository.currentRevision(Cluster.class, cluster.getID());
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

        return new Cluster2PayloadTransformer(baseUri, environment, currentRevision).apply(cluster);
    }

    @GetMapping(path = "/{clustername}/revisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RevisionPayload<Cluster>> getRevisions(
            @PathVariable(name = "environment") String environmentName,
            @PathVariable(name = "clustername") String clustername) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster cluster = findCluster(environment, clustername);
        List<FasitRevision<Cluster>> revisions = revisionRepository.getRevisionsFor(Cluster.class, cluster.getID());

        URI absPath = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

        List<RevisionPayload<Cluster>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(absPath))
                .collect(Collectors.toList());
        return payload;
    }

    @GetMapping(path = "/{clustername}/revisions/{revision}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ClusterPayload getClusterByRevision(@PathVariable(name = "environment") String environmentName, @PathVariable(name = "clustername") String clustername, @PathVariable(name = "revision") Long revision) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster cluster = findCluster(environment, clustername);
        Optional<Cluster> historic = revisionRepository.getRevisionEntry(Cluster.class, cluster.getID(), revision);
        Cluster old = historic.orElseThrow(() -> 
        	new ResponseStatusException(HttpStatus.NOT_FOUND,"Revison " + revision + " is not found for cluster " + clustername + " in environment " + environmentName)
        );
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

        return new Cluster2PayloadTransformer(baseUri, environment, revision).apply(old);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Void> createCluster(@PathVariable(name = "environment") String environmentName, @Valid @RequestBody ClusterPayload payload) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        if (environment.findClusterByName(payload.clusterName) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cluster with name " + payload.clusterName + " already exists in " + environmentName);
        }
        checkAccess(environment);
        log.info("Creating new cluster {} in environment {} ", payload.clusterName, environment.getName());
        Cluster cluster = new Payload2ClusterTransformer(environment, applicationRepository, null).apply(payload);
        environment.addCluster(cluster);
        environmentRepository.save(environment);
        URI clusterUrl = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{clusterName}")
                .buildAndExpand(payload.clusterName)
                .toUri();
        return ResponseEntity.created(clusterUrl).build();
    }

    @PutMapping(path = "/{clustername}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ClusterPayload updateCluster(@PathVariable(name = "environment") String environmentName, @PathVariable(name = "clustername") String clustername, @Valid @RequestBody ClusterPayload payload) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster oldCluster = findCluster(environment, clustername);
        checkAccess(oldCluster);
        log.info("Updating cluster {} in environment {} ", payload.clusterName, environment.getName());


        if (!oldCluster.getName().equals(payload.clusterName) && environment.findClusterByName(payload.clusterName) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cluster with name " + payload.clusterName + " already exists in " + environmentName);
        }
        Cluster updatedCluster = new Payload2ClusterTransformer(environment, applicationRepository, oldCluster).apply(payload);

        lifeCycleSupport.update(oldCluster, payload);
        clusterRepository.save(updatedCluster);

        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return new Cluster2PayloadTransformer(baseUri, environment).apply(updatedCluster);
    }

    @DeleteMapping(path = "/{clustername}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Void> deleteCluster(@PathVariable(name = "environment") String environmentName, @PathVariable(name = "clustername") String clustername) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster cluster = findCluster(environment, clustername);
        checkAccess(cluster);
        deleteCluster(cluster, environment, EntityCommenter.getComment());
        return ResponseEntity.noContent().build();

    }

    // TODO vi må rydde opp i node, environment og cluster. Bare rot sånn det er nå
    protected ResponseEntity<Void> deleteCluster(Cluster cluster, Environment environment, String comment) {
        log.info("Deleting cluster {} from environment {} with comment {}", cluster.getName(), environment.getName(), comment);
        lifeCycleSupport.delete(cluster);
        for (ApplicationInstance applicationInstance : cluster.getApplicationInstances()) {
            // EntityCommenter.getOnBehalfUserOrRealUser(applicationInstance));
            lifeCycleSupport.delete(applicationInstance);
        }
        environment.removeCluster(cluster);
        environmentRepository.save(environment);
        clusterRepository.delete(cluster);
        return ResponseEntity.noContent().build();
    }

    private Cluster findCluster(Environment environment, String clustername) {
        Cluster cluster = environment.findClusterByName(clustername);
        if (cluster == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cluster with name " + clustername + " is not found in environment " + environment.getName());
        }
        return cluster;
    }
}
