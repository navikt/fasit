package no.nav.aura.fasit.rest;

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

@Component
@Path("api/v2/environments/{environment}/clusters")
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

    @Context
    private UriInfo uriInfo;

    public static URI clusterUrl(URI baseUri, Environment environment, Cluster cluster) {
        return UriBuilder.fromUri(baseUri).path(ClusterRest.class).path(ClusterRest.class, "getCluster").build(environment.getName(), cluster.getName());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClusterPayload> findClusters(@PathParam("environment") String environmentName, @QueryParam("status") LifeCycleStatus lifeCycleStatus) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        return environment.getClusters().stream()
                .filter(cluster -> lifeCycleStatus != null ?  lifeCycleStatus.equals(cluster.getLifeCycleStatus()) : true)
                .sorted(Comparator.comparing(Cluster::getName))
                .map(new Cluster2PayloadTransformer(uriInfo, environment))
                .collect(Collectors.toList());
    }

    @GET
    @Path("{clustername}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterPayload getCluster(@PathParam("environment") String environmentName, @PathParam("clustername") String clustername) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster cluster = findCluster(environment, clustername);
        Long currentRevision = revisionRepository.currentRevision(Cluster.class, cluster.getID());
        return new Cluster2PayloadTransformer(uriInfo, environment, currentRevision).apply(cluster);
    }

    @GET
    @Path("{clustername}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RevisionPayload<Cluster>> getRevisions(
            @PathParam("environment") String environmentName,
            @PathParam("clustername") String clustername) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster cluster = findCluster(environment, clustername);
        List<FasitRevision<Cluster>> revisions = revisionRepository.getRevisionsFor(Cluster.class, cluster.getID());

        List<RevisionPayload<Cluster>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(uriInfo.getAbsolutePath()))
                .collect(Collectors.toList());
        return payload;
    }

    @GET
    @Path("{clustername}/revisions/{revision}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterPayload getClusterByRevision(@PathParam("environment") String environmentName, @PathParam("clustername") String clustername, @PathParam("revision") Long revision) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster cluster = findCluster(environment, clustername);
        Optional<Cluster> historic = revisionRepository.getRevisionEntry(Cluster.class, cluster.getID(), revision);
        Cluster old = historic.orElseThrow(() -> new NotFoundException("Revison " + revision + " is not found for cluster " + clustername + " in environment " + environmentName));
        return new Cluster2PayloadTransformer(uriInfo, environment, revision).apply(old);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createCluster(@PathParam("environment") String environmentName, @Valid ClusterPayload payload) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        if (environment.findClusterByName(payload.clusterName) != null) {
            throw new BadRequestException("Cluster with name " + payload.clusterName + " already exists in " + environmentName);
        }
        checkAccess(environment);
        log.info("Creating new cluster {} in environment {} ", payload.clusterName, environment.getName());
        Cluster cluster = new Payload2ClusterTransformer(environment, applicationRepository, null).apply(payload);
        environment.addCluster(cluster);
        environmentRepository.save(environment);
        URI clusterUrl = uriInfo.getAbsolutePathBuilder().path(payload.clusterName).build();
        return Response.created(clusterUrl).build();
    }

    @PUT
    @Path("{clustername}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public ClusterPayload updateCluster(@PathParam("environment") String environmentName, @PathParam("clustername") String clustername, @Valid ClusterPayload payload) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster oldCluster = findCluster(environment, clustername);
        checkAccess(oldCluster);
        log.info("Updating cluster {} in environment {} ", payload.clusterName, environment.getName());


        if (!oldCluster.getName().equals(payload.clusterName) && environment.findClusterByName(payload.clusterName) != null) {
            throw new BadRequestException("Cluster with name " + payload.clusterName + " already exists in " + environmentName);
        }
        Cluster updatedCluster = new Payload2ClusterTransformer(environment, applicationRepository, oldCluster).apply(payload);

        lifeCycleSupport.update(oldCluster, payload);
        clusterRepository.save(updatedCluster);

        return new Cluster2PayloadTransformer(uriInfo, environment).apply(updatedCluster);
    }

    @DELETE
    @Path("{clustername}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public void deleteCluster(@PathParam("environment") String environmentName, @PathParam("clustername") String clustername) {
        Environment environment = validationHelpers.getEnvironment(environmentName);
        Cluster cluster = findCluster(environment, clustername);
        checkAccess(cluster);
        deleteCluster(cluster, environment, EntityCommenter.getComment());
    }

    // TODO vi må rydde opp i node, environment og cluster. Bare rot sånn det er nå
    protected void deleteCluster(Cluster cluster, Environment environment, String comment) {
        log.info("Deleting cluster {} from environment {} with comment {}", cluster.getName(), environment.getName(), comment);
        lifeCycleSupport.delete(cluster);
        for (ApplicationInstance applicationInstance : cluster.getApplicationInstances()) {
            // EntityCommenter.getOnBehalfUserOrRealUser(applicationInstance));
            lifeCycleSupport.delete(applicationInstance);
        }
        environment.removeCluster(cluster);
        environmentRepository.save(environment);
        clusterRepository.delete(cluster);
    }

    private Cluster findCluster(Environment environment, String clustername) {
        Cluster cluster = environment.findClusterByName(clustername);
        if (cluster == null) {
            throw new NotFoundException("Cluster with name " + clustername + " is not found i environment " + environment.getName());
        }
        return cluster;
    }
}
