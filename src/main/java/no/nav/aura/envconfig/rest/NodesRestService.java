package no.nav.aura.envconfig.rest;

import static no.nav.aura.envconfig.rest.util.Converters.toEnumOrNull;
import static no.nav.aura.envconfig.rest.util.Converters.toPlatformType;
import static no.nav.aura.envconfig.rest.util.Converters.toPlatformTypeDO;
import static no.nav.aura.envconfig.util.IpAddressResolver.resolveIpFrom;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.client.LifeCycleStatusDO;
import no.nav.aura.envconfig.client.NodeDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.fasit.repository.NodeRepository;

import no.nav.aura.integration.VeraRestClient;
import org.apache.commons.lang3.StringUtils;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

@Path("/conf/nodes")
@Component
public class NodesRestService {

    private static final Logger logger = LoggerFactory.getLogger(NodesRestService.class);

    private FasitRepository repo;
    private NodeRepository nodeRepository;

    @Inject
    VeraRestClient vera;

    @Context
    UriInfo uriInfo;

    protected NodesRestService() {
        // For cglib transactions
    }

    @Autowired
    public NodesRestService(FasitRepository repo, NodeRepository nodeRepository, VeraRestClient vera) {
        this.repo = repo;
        this.nodeRepository = nodeRepository;
        this.vera = vera;
    }

    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public NodeDO[] searchForNodes(
            @QueryParam("envName") String environmentName,
            @QueryParam("envClass") String environmentClass,
            @QueryParam("domain") String domain,
            @QueryParam("platformType") String platformType, @Context UriInfo uriInfo) {
        List<Node> nodes = null;
        if (environmentName != null && !environmentName.isEmpty()) {
            nodes = nodeRepository.findNodesByEnvironmentName(environmentName);
        } else if (environmentClass != null) {
            EnvironmentClass envClass = toEnumOrNull(EnvironmentClass.class, environmentClass);
            if (envClass == null) {
                throw new BadRequestException(String.format("%s is not a valid envClass", environmentClass));
            }
            nodes = nodeRepository.findNodesByEnvironmentClass(envClass);
        }
        else {
            nodes = nodeRepository.findAllNodes();
        }
        List<Node> filteredNodes = filterNodeList(nodes, domain, platformType);
        List<NodeDO> nodeDOs = new ArrayList<>();

        for (Node node : filteredNodes) {
            nodeDOs.add(createNodeDO(uriInfo, node));
        }
        return nodeDOs.toArray(new NodeDO[0]);
    }

    private ImmutableList<Node> filterNodeList(Iterable<Node> nodes, String domainStr, String platformTypeStr) {
        FluentIterable<Node> filteredNodes = FluentIterable.from(nodes);

        if (domainStr != null && !domainStr.isEmpty()) {
            filteredNodes = applyFilter(filteredNodes, domainFilter(Domain.fromFqdn(domainStr)));
        }
        if (platformTypeStr != null && !platformTypeStr.isEmpty()) {
            final PlatformType platformType = toEnumOrNull(PlatformType.class, platformTypeStr);
            if (platformType == null) {
                throw new BadRequestException(String.format("%s is an invalid platformtype, use %s", platformTypeStr, EnumSet.allOf(PlatformType.class)));
            }
            filteredNodes = applyFilter(filteredNodes, platformTypeFilter(platformType));
        }
        return filteredNodes.toList();
    }

    private FluentIterable<Node> applyFilter(FluentIterable<Node> nodesTofilter, Predicate<Node> predicate) {
        return nodesTofilter.filter(predicate);
    }

    private Predicate<Node> domainFilter(final Domain domain) {
        return new Predicate<Node>() {
            @Override
            public boolean apply(Node input) {
                return input.getDomain().equals(domain);
            }
        };
    }

    private Predicate<Node> platformTypeFilter(final PlatformType platformType) {
        return new Predicate<Node>() {
            @Override
            public boolean apply(Node input) {
                return input.getPlatformType().equals(platformType);
            }
        };
    }

    /**
     * Henter ut informasjon om en gitt node
     * 
     * @param hostname
     * @return info om noden
     * 
     * @HTTP 404 hvis host ikke finnes i fasit
     */
    @GET
    @Path("/{hostname}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public NodeDO getNode(@PathParam("hostname") String hostname, @Context UriInfo uriInfo) {
        Node node = nodeRepository.findNodeByHostName(hostname);
        if (node == null) {
            throw new NotFoundException("Node with hostname " + hostname + " not found");
        }
        return createNodeDO(uriInfo, node);
    }

    /**
     * Sletter noden eller dmgr som finnes med hostnavn
     * 
     * @param hostname
     * 
     * @HTTP 404 hvis host ikke finnes i fasit
     */
    @DELETE
    @Transactional
    @Path("/{hostname}")
    public void deleteNode(@PathParam("hostname") String hostname) {
        DeleteableEntity deleteme = findNodeOrNodeAwareResourceByName(hostname);
        Optional<Cluster> cluster = findCluster(deleteme);
        repo.delete(deleteme);
        deleteClusterIfEmpty(cluster);
        logger.info("deleted node {}", hostname);

    }

    private DeleteableEntity findNodeOrNodeAwareResourceByName(String hostname) {
        Node node = nodeRepository.findNodeByHostName(hostname);
        if (node != null) {
            return node;
        }
        for (ResourceType hostDependentType : ResourceType.serverDependentResourceTypes) {
            List<Resource> hostDependentResource = repo.findResourcesByExactAlias(new Scope(), hostDependentType, null);
            for (Resource resource : hostDependentResource) {
                String resourceHostname = resource.getProperties().get("hostname");
                if (hostname.equalsIgnoreCase(resourceHostname)) {
                    return resource;
                }
            }
        }

        throw new NotFoundException("Node with hostname " + hostname + " not found");
    }

    private Optional<Cluster> findCluster(DeleteableEntity deleteme) {
        if (deleteme instanceof Node) {
            return Optional.fromNullable(nodeRepository.findClusterByNode((Node) deleteme));
        }
        return Optional.absent();
    }

    private void deleteClusterIfEmpty(Optional<Cluster> cluster) {
        if (cluster.isPresent() && cluster.get().getNodes().isEmpty()) {
            logger.info("Cluster {} is now empty, deleting cluster", cluster.get().getName());
            Cluster storedCluster = repo.getById(Cluster.class, cluster.get().getID());
            Environment environment = repo.getEnvironmentBy(storedCluster);
            Set<ApplicationInstance> applicationInstances = storedCluster.getApplicationInstances();
            for (ApplicationInstance applicationInstance : applicationInstances) {
                vera.notifyVeraOfUndeployment(applicationInstance.getApplication().getName(), environment.getName(), EntityCommenter.getOnBehalfUserOrRealUser(applicationInstance));
            }
            repo.delete(storedCluster);

        }
    }

    /**
     * Oppdaterer noden
     * 
     * @param hostname
     * 
     * @HTTP 404 hvis host ikke finnes i fasit
     */
    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_XML)
    @Path("/{hostname}")
    public Response updateNode(@PathParam("hostname") String hostname, final NodeDO nodeDO) {
        if (nodeDO.getStatus() == null) {
            throw new BadRequestException("Required property status in node is not set or is illegal");
        }
        DeleteableEntity updateMe = findNodeOrNodeAwareResourceByName(hostname);
        if (LifeCycleStatusDO.STOPPED == nodeDO.getStatus()) {
            updateMe.changeStatus(LifeCycleStatus.STOPPED);
            repo.store(updateMe);
            logger.info("Stopping node {}", hostname);

        }
        if (LifeCycleStatusDO.STARTED == nodeDO.getStatus()) {
            updateMe.resetStatus();
            repo.store(updateMe);
            logger.info("Starting node {}", hostname);
        }

        return Response.created(uriInfo.getBaseUriBuilder().clone().path(NodesRestService.class, "getNode").build(hostname)).build();
    }

    @PUT
    @Transactional
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public NodeDO registerNode(final NodeDO nodeDO) {
        String envName = nodeDO.getEnvironmentName();

        if (nodeDO.getDomain() == null || envName == null || StringUtils.isEmpty(nodeDO.getApplicationMappingName())) {
            throw new IllegalArgumentException("Domain, environmentName and applicationMappingName mandatory");
        }
        Domain domain = Domain.fromFqdn(nodeDO.getDomain().getFqn());
        final Environment environment = getEnvironment(envName);
        Node node = createNode(nodeDO, domain);
        String applicationMappingName = nodeDO.getApplicationMappingName();

        Cluster cluster = getOrCreateCluster(applicationMappingName, environment, domain);

        node.setPlatformType(toPlatformType(nodeDO.getPlatformType()));
        environment.addNode(cluster, node);

        repo.store(environment);

        return createNodeDO(uriInfo, nodeRepository.findNodeByHostName(nodeDO.getHostname()));
    }

    private Node createNode(NodeDO nodeDO, Domain domain) {
        String hostname = nodeDO.getHostname();
        if (nodeRepository.findNodeByHostName(hostname) != null) {
            throw new BadRequestException("Node " + hostname + " already exists in Fasit, hostname must be unique");
        }

        Node node = new Node(hostname, nodeDO.getUsername(), nodeDO.getPassword());

        if (nodeDO.getAccessAdGroup() != null) {
            node.getAccessControl().setAdGroups(nodeDO.getAccessAdGroup());
        }

        if (node.getDomain() != domain) {
            throw new IllegalArgumentException("Hostname " + node.getHostname() + " does not match domain name " + domain);
        }
        return node;
    }

    private Cluster getOrCreateCluster(String applicationMappingName, final Environment environment, final Domain domain) {
        if (isApplicationGroup(applicationMappingName)) {
            ApplicationGroup applicationGroup = repo.findApplicationGroupByName(applicationMappingName);
            if (applicationGroup.getApplications().isEmpty()) {
                throw new IllegalArgumentException("Applicationgroup " + applicationMappingName + " contains no applications");
            }
            ApplicationInstance firstAppInstance = environment.findApplicationByName(applicationGroup.getApplications().iterator().next().getName());
            if (firstAppInstance != null) {
                Cluster cluster = firstAppInstance.getCluster();
                if (!cluster.getDomain().equals(domain)) {
                    throw new IllegalArgumentException(
                            String.format("Cluster already exists in a different domain. " +
                                    "Existing cluster: %s in %s New cluster in %s." +
                                    " Fix this by manually deleting the cluster created in the wrong domain in Fasit",
                                    cluster.getName(), cluster.getDomain().getNameWithZone(), domain.getNameWithZone()));
                }
                return cluster;

            }

            return createClusterForApplicationGroup(environment, applicationGroup, domain);
        }
        else {
            ApplicationInstance applicationInstance = environment.findApplicationByName(applicationMappingName);

            if (applicationInstance != null) {
                return applicationInstance.getCluster();
            }
            Application application = repo.findApplicationByName(applicationMappingName);

            if (application == null) {
                throw new BadRequestException("Application " + applicationMappingName + " does not exist in Fasit");
            }

            return createClusterForApplication(environment, application, domain);
        }
    }

    private Cluster createClusterForApplication(Environment environment, Application application, Domain domain) {
        Cluster cluster = createCluster(environment, application.getName(), domain);
        logger.info("Adding application {} to new cluster ", application.getName());
        cluster.addApplication(application);
        return cluster;
    }

    private Cluster createClusterForApplicationGroup(Environment environment, ApplicationGroup applicationGroup, Domain domain) {
        String name = applicationGroup.getName();
        Cluster cluster = createCluster(environment, name, domain);
        logger.info("Adding application group {} {} to new cluster ", applicationGroup.getName(), applicationGroup.getApplications());
        for (Application application : applicationGroup.getApplications()) {
            cluster.addApplication(application);
        }

        return cluster;
    }

    private boolean isApplicationGroup(String applicationMappingName) {
        ApplicationGroup applicationGroup = repo.findApplicationGroupByName(applicationMappingName);
        return applicationGroup != null;
    }

    private Cluster createCluster(Environment environment, String clusterName, Domain domain) {
        logger.info("Creating cluster {}", clusterName + "Cluster");
        return environment.addCluster(new Cluster(clusterName + "Cluster", domain));
    }

    public Environment getEnvironment(String envName) {
        Environment environment = repo.findEnvironmentBy(envName.toLowerCase());
        if (environment == null) {
            throw new BadRequestException(String.format("Environment %s does not exist in Fasit, unable to register node", envName));
        }
        return environment;
    }

    public NodeDO createNodeDO(UriInfo uriInfo, Node node) {
        NodeDO nodeDO = new NodeDO();
        nodeDO.setRef(uriInfo.getBaseUriBuilder().clone().path(NodesRestService.class).path(NodesRestService.class, "getNode").build(node.getHostname()));
        nodeDO.setHostname(node.getHostname());
        nodeDO.setUsername(node.getUsername());
        URI ref = UriBuilder.fromUri(uriInfo.getBaseUri()).path(SecretRestService.createPath(node.getPassword())).build();
        nodeDO.setPasswordRef(ref);
        nodeDO.setDomain(findDomainOrNull(node));
        nodeDO.setPlatformType(toPlatformTypeDO(node.getPlatformType()));
        nodeDO.setIpAddress(resolveIpFrom(node.getHostname()).orNull());
        Optional<Cluster> cluster = FluentIterable.from(node.getClusters()).first();
        if (cluster.isPresent() && !cluster.get().getApplications().isEmpty()) {
            Collection<Application> applications = cluster.get().getApplications();
            nodeDO.setApplicationMappingName(getApplicationMappingName(applications));
            nodeDO.setApplicationName(getApplicationNames(applications));
        }

        Environment environment = nodeRepository.findEnvironment(node);
        if (environment != null) {
            nodeDO.setEnvironmentName(environment.getName());
            nodeDO.setEnvironmentClass(environment.getEnvClass().name());
        }
        if (node.getLifeCycleStatus() != null) {
            nodeDO.setStatus(LifeCycleStatusDO.valueOf(node.getLifeCycleStatus().name()));
        }
        nodeDO.setAccessAdGroup(node.getAccessControl().getAdGroups());
        return nodeDO;
    }

    private String[] getApplicationNames(Collection<Application> applications) {
        return FluentIterable.from(applications).transform(new Function<Application, String>() {

            @Override
            public String apply(Application input) {
                return input.getName();
            }
        }).toArray(String.class);
    }

    private String getApplicationMappingName(Collection<Application> applications) {
        if (applications.isEmpty()) {
            return null;
        }
        Application firstApplication = applications.iterator().next();
        String mappingName = firstApplication.getName();
        if (applications.size() > 1) {
            ApplicationGroup applicationGroup = repo.findApplicationGroup(firstApplication);
            if (applicationGroup != null) {
                mappingName = applicationGroup.getName();
            }
        }
        return mappingName;
    }

    private String findDomainOrNull(Node node) {
        try {
            return node.getDomain().getFqn();
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            return null;
        }
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

}
