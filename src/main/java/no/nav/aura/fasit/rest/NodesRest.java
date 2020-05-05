package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.repository.specs.NodeSpecs;
import no.nav.aura.fasit.rest.converter.Node2PayloadTransformer;
import no.nav.aura.fasit.rest.converter.Payload2NodeTransformer;
import no.nav.aura.fasit.rest.converter.Revision2PayloadTransformer;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.helpers.ValidationHelpers;
import no.nav.aura.fasit.rest.model.Link;
import no.nav.aura.fasit.rest.model.NodePayload;
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
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.aura.fasit.rest.helpers.PagingBuilder.pagingResponseBuilder;
import static no.nav.aura.fasit.rest.security.AccessChecker.checkAccess;

@Component
@Path("api/v2/nodes")
public class NodesRest {

    @Inject
    private NodeRepository nodeRepository;
    @Inject
    private EnvironmentRepository environmentRepository;
    @Inject
    private ApplicationRepository applicationRepository;
    @Inject
    private RevisionRepository revisionRepository;
    @Inject
    private LifeCycleSupport lifeCycleSupport;
    @Inject
    private ValidationHelpers validationHelpers;

    @Inject
    private ClusterRest clusterService;

    private final static Logger log = LoggerFactory.getLogger(NodesRest.class);

    @Context
    private UriInfo uriInfo;
    
    public static URI nodeUrl(URI baseUri, String hostname){
        return UriBuilder.fromUri(baseUri).path(NodesRest.class).path(NodesRest.class, "getNode").build(hostname);
    }

    public NodesRest() {
    }
    

    public NodesRest(NodeRepository nodeRepository, EnvironmentRepository environmentRepository, ApplicationRepository applicationRepository, RevisionRepository revisionRepository,
            LifeCycleSupport lifeCycleSupport, ClusterRest clusterRest) {
        this.nodeRepository = nodeRepository;
        this.environmentRepository = environmentRepository;
        this.applicationRepository = applicationRepository;
        this.revisionRepository = revisionRepository;
        this.lifeCycleSupport = lifeCycleSupport;
        this.clusterService = clusterRest;
    }

    /**
     * find nodes by query params or list all nodes. Hostname query param supports like search
     * @responseType java.util.List<no.nav.aura.fasit.rest.model.NodePayload>
     * */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findNodes(
            @QueryParam("environment") String environmentName,
            @QueryParam("environmentclass") EnvironmentClass environmentClass,
            @QueryParam("type") PlatformType type,
            @QueryParam("hostname") String hostname,
            @QueryParam("application") String application,
            @QueryParam("status") LifeCycleStatus lifeCycleStatus,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pr_page") @DefaultValue("100") int pr_page) {
        long start = System.currentTimeMillis();
        Specification<Node> spec = NodeSpecs.find(environmentName, environmentClass, type, hostname, application, lifeCycleStatus);
        PageRequest pageRequest = new PageRequest(page, pr_page);
        Page<Node> nodes = null;
        if (spec != null) {
            nodes = nodeRepository.findAll(spec, pageRequest);
        } else
            nodes = nodeRepository.findAll(pageRequest);

        Node2PayloadTransformer transformer = new Node2PayloadTransformer(nodeRepository, uriInfo.getBaseUri());
        List<NodePayload> result = nodes.getContent().stream()
                .map(transformer)
                .collect(Collectors.toList());
        log.info("finding {} nodes time: {} ", nodes.getNumberOfElements(), (System.currentTimeMillis() - start));
        return pagingResponseBuilder(nodes, uriInfo.getRequestUri()).entity(result).build();
    }

    /**
     * Returns a list of currently available node types
     * */
    @GET
    @Path("types")
    @Produces(MediaType.APPLICATION_JSON)
    public PlatformType[] getNodeType() {
        return PlatformType.values();
    }

    /**
     * find node by hostname
     * @responseMessage 404 Node with hostname not found
     * */
    @GET
    @Path("{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public NodePayload getNode(@PathParam("hostname") String hostname) {
        Node node = getNodeByHostname(hostname);
        Long currentRevision = revisionRepository.currentRevision(Node.class, node.getID());
        return new Node2PayloadTransformer(nodeRepository, uriInfo.getBaseUri(), currentRevision).apply(node);
    }


    /**
     * show revision history for a given node
     * @responseMessage 404 Node with hostname not found
     * */
    @GET
    @Path("{hostname}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RevisionPayload<Node>> getRevisions(
            @PathParam("hostname") String hostname) {
        Node node = getNodeByHostname(hostname);
        List<FasitRevision<Node>> revisions = revisionRepository.getRevisionsFor(Node.class, node.getID());

        List<RevisionPayload<Node>> payload = revisions.stream()
                .map(new Revision2PayloadTransformer<>(uriInfo.getAbsolutePath()))
                .collect(Collectors.toList());
        return payload;
    }

    /**
     * show revision info for a given node and a given revision number
     * @responseMessage 404 Node with hostname not found
     * @responseMessage 404 Revision number not found
     * */
    @GET
    @Path("{hostname}/revisions/{revision}")
    @Produces(MediaType.APPLICATION_JSON)
    public NodePayload getNodeByRevision(@PathParam("hostname") String hostname, @PathParam("revision") Long revision) {
        Node node = getNodeByHostname(hostname);
        Optional<Node> historicNode = revisionRepository.getRevisionEntry(Node.class, node.getID(), revision);
        Node oldNode = historicNode.orElseThrow(() -> new NotFoundException("Revison " + revision + " is not found for node " + hostname));
        return new Node2PayloadTransformer(nodeRepository, uriInfo.getBaseUri(), revision).apply(oldNode);
    }


    /**
     * Register a new node and map it to a cluster. If cluster does not exist, a new cluster is created
     * @responseMessage 400 Hostname already exist
     * @responseMessage 400 Environment name in payload does not exist
     * @responseMessage 400 Application name in payload does not exist
     * @responseMessage 403 Authenticated user does not have access to registering a node in this environment
     * @responseType no.nav.aura.fasit.rest.model.NodePayload
     * */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createNode(@Valid NodePayload payload) {
        if (nodeRepository.findNodeByHostName(payload.hostname) != null) {
            throw new BadRequestException("Node with hostname " + payload.hostname + " already exists in Fasit ");
        }
        if (payload.password == null){
            throw new BadRequestException("password is required for creating a new node");
        }

        Environment environment = validationHelpers.getEnvironment(payload.environment);
        Node node = new Payload2NodeTransformer().apply(payload);
        checkAccess(node);


        if(payload.cluster.size() ==  0 ) {
            createOrUpdateCluster(payload, environment, node, null);
        }

        for(Link clusterLink : payload.cluster) {
            createOrUpdateCluster(payload, environment, node, clusterLink);
        }

        environment.addNode(node);
        environmentRepository.save(environment);
        URI nodeUrl = uriInfo.getAbsolutePathBuilder().path(payload.hostname).build();
        return Response.created(nodeUrl).build();
    }

    private void createOrUpdateCluster( NodePayload payload, Environment environment, Node node, Link clusterLink) {
        Optional<Cluster> existingCluster = findCluster(clusterLink, environment);

        Cluster cluster = existingCluster.orElse(createCluster(clusterLink, payload, environment));
        cluster.addNode(node);

        for (String appName : payload.applications) {
            if (cluster.getApplications().stream().noneMatch(app -> app.getName().equalsIgnoreCase(appName))) {
                Application application = applicationRepository.findByNameIgnoreCase(appName);
                if (application == null) {
                    throw new BadRequestException("Application " + appName + " does not exist in Fasit");
                }
                cluster.addApplication(application);
            }
        }
    }


    /**
     * Update a given node
     * @responseMessage 400 Hostname in path differs from hostname in payload
     * @responseMessage 404 Hostname does not exist
     * @responseMessage 403 Authenticated user does not have access to updating a node in this environment
     * */
    @PUT
    @Path("{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public NodePayload updateNode(@PathParam("hostname") String hostname, @Valid NodePayload payload) {
        if (!hostname.equals(payload.hostname)) {
            throw new BadRequestException("Hostname in payload and in path must be equal. Got " + payload.hostname + " and " + hostname);
        }
        Node node = getNodeByHostname(hostname);
        checkAccess(node);
        // TODO sjekke om det finnes en ressurs som denne noden....
        Node updatedNode = new Payload2NodeTransformer(node).apply(payload);
        lifeCycleSupport.update(updatedNode, payload);
        log.info("updated node {}", hostname);
        nodeRepository.save(updatedNode);

        return new Node2PayloadTransformer(nodeRepository, uriInfo.getBaseUri()).apply(updatedNode);
    }

    /**
     * Delete a given node
     * @responseMessage 404 Hostname does not exist
     * @responseMessage 403 Authenticated user does not have access to registering a node in this environment
     *
     * */
    @DELETE
    @Path("{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public void deleteNode(@PathParam("hostname") String hostname) {
        Node node = getNodeByHostname(hostname);
        checkAccess(node);
        Environment environment = nodeRepository.findEnvironment(node);
        if (environment != null) {
            environment.removeNode(node);
            environmentRepository.save(environment);
        }

        lifeCycleSupport.delete(node);
        log.info("deleted node {}", hostname);
        nodeRepository.delete(node);
    }

    private Node getNodeByHostname(String hostname) {
        Node node = nodeRepository.findNodeByHostName(hostname);
        if (node != null) {
            return node;
        }
        throw new NotFoundException("Node with hostname " + hostname + " does not exist in Fasit");
    }

    private Cluster createCluster(Link clusterLink, NodePayload payload, Environment environment) {
        String clusterNameSuggestion;


        if (clusterLink != null && clusterLink.getName() != null){
            clusterNameSuggestion = clusterLink.getName();
        } else {
            clusterNameSuggestion = "cluster" + (environment.getClusters().size() + 1);
        }
        Optional<String> firstApp = findFistApplicationName(payload);
        if (firstApp.isPresent()) {
            clusterNameSuggestion = firstApp.get() + "Cluster";
        }
        String clusterName = generateNewClusterName(clusterNameSuggestion, environment);
        Domain domain = Domain.from(payload.environmentClass, payload.zone);
        Cluster cluster = new Cluster(clusterName, domain);
        environment.addCluster(cluster);
        log.info("Created new cluster {} in environment {}", clusterName, environment.getName());
        return cluster;
    }

    private String generateNewClusterName(final String suggestion, Environment environment) {
        if (environment.getClusters().stream().anyMatch(c -> c.getName().equalsIgnoreCase(suggestion))) {
            log.info("Generating new clustername because {} is already in {}", suggestion, environment);
            return generateNewClusterName(suggestion + "_" + Math.random(), environment);
        }
        return suggestion;
    }

    private Optional<String>  findFistApplicationName(NodePayload payload) {
        return payload.applications.stream().findFirst();
    }

    private Optional<Cluster> findCluster(Link clusterLink, Environment environment) {
        if (clusterLink == null) {
            return Optional.empty();
        }
        Cluster cluster = environment.findClusterByName(clusterLink.getName());
        return Optional.ofNullable(cluster);
    }
}
