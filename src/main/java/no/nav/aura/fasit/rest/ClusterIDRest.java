package no.nav.aura.fasit.rest;

import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.auditing.FasitRevision;
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


/*
* This is an extra Cluster rest service. The other rest service only works with environmentname and clustername.
* This does not work well from when we need to edit the clustername.
* This service works on cluster ID instead. Services for searching for clusters and creating clusters are still in ClusterRest
* */
@RestController
@RequestMapping(path = "/api/v2/clusters")
public class ClusterIDRest {
    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private ClusterRest clusterRest;

    @Inject
    private ApplicationRepository applicationRepository;

    @Inject
    private EnvironmentRepository environmentRepository;

    @Inject
    private LifeCycleSupport lifeCycleSupport;
    @Inject
    private RevisionRepository revisionRepository;
    @Inject
    private ValidationHelpers validationHelpers;


    private final static Logger log = LoggerFactory.getLogger(ClusterIDRest.class);

    public static URI clusterUrl(URI baseUri, Cluster cluster) {
        return ServletUriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/clusters/{id}")
                .buildAndExpand(cluster.getID())
                .toUri();
    }


    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ClusterPayload getCluster( @PathVariable("id") Long clusterId) {
        Cluster cluster = findCluster(clusterId);
        Environment environment = clusterRepository.findEnvironment(cluster);

        Long currentRevision = revisionRepository.currentRevision(Cluster.class, cluster.getID());
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();

        return new Cluster2PayloadTransformer(baseUri, environment, currentRevision).apply(cluster);
    }



    @GetMapping(path = "/{id}/revisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RevisionPayload<Cluster>> getRevisions(@PathVariable(name = "id") Long clusterId) {
        Cluster cluster = findCluster(clusterId);
        List<FasitRevision<Cluster>> revisions = revisionRepository.getRevisionsFor(Cluster.class, cluster.getID());

        URI absPath = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        List<RevisionPayload<Cluster>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(absPath))
                .collect(Collectors.toList());
        return payload;
    }

    @GetMapping(path = "/{id}/revisions/{revision}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ClusterPayload getClusterByRevision(
    		@PathVariable(name = "id") Long clusterId, 
    		@PathVariable(name = "revision") Long revision) {
        Cluster cluster = findCluster(clusterId);
        Optional<Cluster> historic = revisionRepository.getRevisionEntry(Cluster.class, cluster.getID(), revision);
        Cluster old = historic.orElseThrow(() ->  new ResponseStatusException(HttpStatus.NOT_FOUND, "Revison " + revision + " is not found for cluster " + clusterId));
        Environment environment = clusterRepository.findEnvironment(cluster);

        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return new Cluster2PayloadTransformer(baseUri, environment, revision).apply(old);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ClusterPayload updateCluster(@PathVariable(name = "id") Long clusterId, @Valid @RequestBody ClusterPayload payload) {
        Cluster oldCluster = findCluster(clusterId);
        validationHelpers.getEnvironment(payload.environment);
        checkAccess(oldCluster);
        log.debug("Updating cluster {} in environment {} ", payload.clusterName);

        Environment environment = environmentRepository.findByNameIgnoreCase(payload.environment);

        if (!oldCluster.getName().equals(payload.clusterName) && environment.findClusterByName(payload.clusterName) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cluster with name " + payload.clusterName + " already exists in " + environment.getName());
        }
        Cluster updatedCluster = new Payload2ClusterTransformer(environment, applicationRepository, oldCluster).apply(payload);

        lifeCycleSupport.update(oldCluster, payload);
        clusterRepository.save(updatedCluster);
        URI baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        return new Cluster2PayloadTransformer(baseUri, environment).apply(updatedCluster);
    }

    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Void> deleteCluster(@PathVariable(name = "id") Long clusterId) {
        Cluster cluster = findCluster(clusterId);
        checkAccess(cluster);
        Environment environment = clusterRepository.findEnvironment(cluster);
        clusterRest.deleteCluster(cluster, environment, EntityCommenter.getComment());
        return ResponseEntity.noContent().build();
    }

    private Cluster findCluster(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No cluster found with id " + clusterId)
        );

        return cluster;
    }
}
