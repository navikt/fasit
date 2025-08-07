package no.nav.aura.envconfig.rest;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.auditing.EntityCommenter;
import no.nav.aura.envconfig.client.LifeCycleStatusDO;
import no.nav.aura.envconfig.client.NodeDO;
import no.nav.aura.envconfig.client.NodeListDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.fasit.repository.NodeRepository;
import no.nav.aura.integration.VeraRestClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.net.URI;
import java.util.*;

import static no.nav.aura.envconfig.rest.util.Converters.*;
import static no.nav.aura.envconfig.util.IpAddressResolver.resolveIpFrom;

@RestController
@RequestMapping(path = "/conf/nodes")
public class NodesRestService {

    private static final Logger logger = LoggerFactory.getLogger(NodesRestService.class);


    private final FasitRepository repo;
    private final NodeRepository nodeRepository;
    private final VeraRestClient vera;
    
    protected NodesRestService() {
		this.repo = null;
		this.nodeRepository = null;
		this.vera = null;
        // For cglib transactions
    }

    @Autowired
    public NodesRestService(FasitRepository repo, NodeRepository nodeRepository, VeraRestClient vera) {
        this.repo = repo;
        this.nodeRepository = nodeRepository;
        this.vera = vera;
    }

    @GetMapping(produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public NodeListDO searchForNodes(
    		@RequestParam(name = "envName", required = false) String environmentName,
    		@RequestParam(name = "envClass", required = false) String environmentClass,
    		@RequestParam(name = "domain", required = false) String domain,
    		@RequestParam(name = "platformType", required = false) String platformType) {
    	logger.info("Searching for nodes with envName={}, envClass={}, domain={}, platformType={}", environmentName, environmentClass, domain, platformType);
        List<Node> nodes = null;
        if (environmentName != null && !environmentName.isEmpty()) {
            nodes = nodeRepository.findNodesByEnvironmentName(environmentName);
        } else if (environmentClass != null) {
            EnvironmentClass envClass = toEnumOrNull(EnvironmentClass.class, environmentClass);
            if (envClass == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("%s is not a valid envClass", environmentClass));
            }
            nodes = nodeRepository.findNodesByEnvironmentClass(envClass);
        }
        else {
            nodes = nodeRepository.findAllNodes();
        }
        List<Node> filteredNodes = filterNodeList(nodes, domain, platformType);
        List<NodeDO> nodeDOs = new ArrayList<>();

        for (Node node : filteredNodes) {
            nodeDOs.add(createNodeDO(node));
        }
        NodeListDO nodeListDO = new NodeListDO(nodeDOs);
//        return nodeDOs.toArray(new NodeDO[0]);
        return nodeListDO;
    }

    private ImmutableList<Node> filterNodeList(Iterable<Node> nodes, String domainStr, String platformTypeStr) {
        FluentIterable<Node> filteredNodes = FluentIterable.from(nodes);

        if (domainStr != null && !domainStr.isEmpty()) {
            filteredNodes = applyFilter(filteredNodes, domainFilter(Domain.fromFqdn(domainStr)));
        }
        if (platformTypeStr != null && !platformTypeStr.isEmpty()) {
            final PlatformType platformType = toEnumOrNull(PlatformType.class, platformTypeStr);
            if (platformType == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("%s is an invalid platformtype, use %s", platformTypeStr, EnumSet.allOf(PlatformType.class)));
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
    @GetMapping(path = "/{hostname}", produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public NodeDO getNode(@PathVariable(name = "hostname") String hostname) {
    	logger.info("Getting node with hostname: {}", hostname);
        Node node = nodeRepository.findNodeByHostName(hostname);
        if (node == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node with hostname " + hostname + " not found");
        }
        logger.warn("Found nodes: " + node.getName());
        return createNodeDO(node);
    }

    /**
     * Sletter noden eller dmgr som finnes med hostnavn
     * 
     * @param hostname
     * 
     * @HTTP 404 hvis host ikke finnes i fasit
     */
    @Transactional
    @DeleteMapping(path = "/{hostname}")
    public ResponseEntity<Void>  deleteNode(@PathVariable(name = "hostname") String hostname) {
        DeleteableEntity deleteme = findNodeOrNodeAwareResourceByName(hostname);
        Optional<Cluster> cluster = findCluster(deleteme);
        repo.delete(deleteme);
        deleteClusterIfEmpty(cluster);
        logger.info("deleted node {}", hostname);
        return ResponseEntity.noContent().build();
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

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node with hostname " + hostname + " not found");
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
    @Transactional
    @PostMapping(path = "/{hostname}", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<Void> updateNode(
    		@PathVariable(name = "hostname") String hostname,
    		@RequestBody NodeDO nodeDO) {
        if (nodeDO.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required property status in node is not set or is illegal");
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

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/conf/nodes/{hostname}")
                .buildAndExpand(hostname)
                .toUri();
                
            return ResponseEntity.created(location).build();
    }

    @Transactional
    @PutMapping(consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public NodeDO registerNode(@RequestBody NodeDO nodeDO) {
        String envName = nodeDO.getEnvironmentName();
        if (nodeDO.getDomain() == null || envName == null || StringUtils.isEmpty(nodeDO.getApplicationMappingName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Domain, environmentName and applicationMappingName mandatory");
        }
        Domain domain = Domain.fromFqdn(nodeDO.getDomain().getFqn());
        final Environment environment = getEnvironment(envName);
        Node node = createNode(nodeDO, domain);
        String applicationMappingName = nodeDO.getApplicationMappingName();

        Cluster cluster = getOrCreateCluster(applicationMappingName, environment, domain);

        node.setPlatformType(toPlatformType(nodeDO.getPlatformType()));
        environment.addNode(cluster, node);

        repo.store(environment);

        return createNodeDO(nodeRepository.findNodeByHostName(nodeDO.getHostname()));
    }

    private Node createNode(NodeDO nodeDO, Domain domain) {
        String hostname = nodeDO.getHostname();
        if (nodeRepository.findNodeByHostName(hostname) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Node " + hostname + " already exists in Fasit, hostname must be unique");
        }

        Node node = new Node(hostname, nodeDO.getUsername(), nodeDO.getPassword());

        if (nodeDO.getAccessAdGroup() != null) {
            node.getAccessControl().setAdGroups(nodeDO.getAccessAdGroup());
        }

        if (node.getDomain() != domain) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hostname " + node.getHostname() + " does not match domain name " + domain);
        }
        return node;
    }

    private Cluster getOrCreateCluster(String applicationMappingName, final Environment environment, final Domain domain) {
        if (isApplicationGroup(applicationMappingName)) {
            ApplicationGroup applicationGroup = repo.findApplicationGroupByName(applicationMappingName);
            if (applicationGroup.getApplications().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Applicationgroup " + applicationMappingName + " contains no applications");
            }
            ApplicationInstance firstAppInstance = environment.findApplicationByName(applicationGroup.getApplications().iterator().next().getName());
            if (firstAppInstance != null) {
                Cluster cluster = firstAppInstance.getCluster();
                if (!cluster.getDomain().equals(domain)) {
                	throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application " + applicationMappingName + " does not exist in Fasit");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Environment " + envName + " not found");
        }
        return environment;
    }

    public NodeDO createNodeDO(Node node) {
        NodeDO nodeDO = new NodeDO();
        nodeDO.setRef(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/conf/nodes/{hostname}")
                .buildAndExpand(node.getHostname())
                .toUri());
        nodeDO.setHostname(node.getHostname());
        nodeDO.setUsername(node.getUsername());
        URI ref = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(SecretRestService.createPath(node.getPassword()))
                .build()
                .toUri();
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

}
