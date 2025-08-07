package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.auditing.FasitRevision;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;


/*
* This is an extra Cluster rest service. The other rest service only works with environmentname and clustername.
* This does not work well from when we need to edit the clustername.
* This service works on cluster ID instead. Services for searching for clusters and creating clusters are still in ClusterRest
* */
@Component
@Path("api/v2/clusters")
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

    @Context
    private UriInfo uriInfo;

    public static URI clusterUrl(URI baseUri, Cluster cluster) {
        return UriBuilder.fromUri(baseUri).path(ClusterIDRest.class).path(ClusterIDRest.class, "getCluster").build(cluster.getID());
    }


    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterPayload getCluster( @PathParam("id") Long clusterId) {
        Cluster cluster = findCluster(clusterId);
        Environment environment = clusterRepository.findEnvironment(cluster);

        Long currentRevision = revisionRepository.currentRevision(Cluster.class, cluster.getID());
        return new Cluster2PayloadTransformer(uriInfo, environment, currentRevision).apply(cluster);
    }



    @GET
    @Path("{id}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RevisionPayload<Cluster>> getRevisions(@PathParam("id") Long clusterId) {
        Cluster cluster = findCluster(clusterId);
        List<FasitRevision<Cluster>> revisions = revisionRepository.getRevisionsFor(Cluster.class, cluster.getID());

        List<RevisionPayload<Cluster>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(uriInfo.getAbsolutePath()))
                .collect(Collectors.toList());
        return payload;
    }

    @GET
    @Path("{id}/revisions/{revision}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterPayload getClusterByRevision(@PathParam("id") Long clusterId, @PathParam("revision") Long revision) {
        Cluster cluster = findCluster(clusterId);
        Optional<Cluster> historic = revisionRepository.getRevisionEntry(Cluster.class, cluster.getID(), revision);
        Cluster old = historic.orElseThrow(() -> new NotFoundException("Revison " + revision + " is not found for cluster " + clusterId ));
        Environment environment = clusterRepository.findEnvironment(cluster);

        return new Cluster2PayloadTransformer(uriInfo, environment, revision).apply(old);
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public ClusterPayload updateCluster(@PathParam("id") Long clusterId, @Valid ClusterPayload payload) {
        Cluster oldCluster = findCluster(clusterId);
        validationHelpers.getEnvironment(payload.environment);
        checkAccess(oldCluster);
        log.debug("Updating cluster {} in environment {} ", payload.clusterName);

        Environment environment = environmentRepository.findByNameIgnoreCase(payload.environment);

        if (!oldCluster.getName().equals(payload.clusterName) && environment.findClusterByName(payload.clusterName) != null) {
            throw new BadRequestException("Cluster with name " + payload.clusterName + " already exists in " + environment.getName());
        }
        Cluster updatedCluster = new Payload2ClusterTransformer(environment, applicationRepository, oldCluster).apply(payload);

        lifeCycleSupport.update(oldCluster, payload);
        clusterRepository.save(updatedCluster);

        return new Cluster2PayloadTransformer(uriInfo, environment).apply(updatedCluster);
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public void deleteCluster(@PathParam("id") Long clusterId) {
        Cluster cluster = findCluster(clusterId);
        checkAccess(cluster);
        Environment environment = clusterRepository.findEnvironment(cluster);
        clusterRest.deleteCluster(cluster, environment, EntityCommenter.getComment());
    }

    private Cluster findCluster(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId).orElseThrow(() ->
                new NotFoundException("No cluster found with id " + clusterId)
        );

        return cluster;
    }
}
