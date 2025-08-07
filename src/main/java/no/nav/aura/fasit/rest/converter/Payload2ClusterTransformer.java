package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.rest.model.ClusterPayload;
import no.nav.aura.fasit.rest.model.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Payload2ClusterTransformer extends FromPayloadTransformer<ClusterPayload, Cluster> {
    
    private final Logger log = LoggerFactory.getLogger(Payload2ClusterTransformer.class);

    private final Optional<Cluster> defaultValue;
    private Environment environment;
    private ApplicationRepository applicationRepository;

    public Payload2ClusterTransformer(Environment environment, ApplicationRepository applicationRepository, Cluster defaultValue) {
        this.environment = environment;
        this.applicationRepository = applicationRepository;
        this.defaultValue = Optional.ofNullable(defaultValue);
    }

    @Override
    protected Cluster transform(ClusterPayload payload) {
        Cluster cluster = defaultValue.orElse(new Cluster(payload.clusterName, Domain.from(environment.getEnvClass(), payload.zone)));
        cluster.setName(payload.clusterName);
        cluster.setDomain(Domain.from(environment.getEnvClass(), payload.zone));
        optional(payload.loadBalancerUrl).ifPresent(p -> cluster.setLoadBalancerUrl(p));
        
        optional(payload.nodes).ifPresent(nodes ->updateNodeMapping(nodes, environment, cluster));
        optional(payload.applications).ifPresent(apps -> updateApplicationMappings(apps, environment, cluster));
        return cluster;
    }
    
    private void updateApplicationMappings(Set<Link> applications, Environment environment, Cluster cluster) {
        Set<String> appsInCluster = cluster.getApplications().stream()
                .map(a -> a.getName())
                .collect(Collectors.toSet());
        Set<String> appsInEnvironmentNotInCluster = environment.getApplications().stream()
                .map(a -> a.getName())
                .filter(a -> !appsInCluster.contains(a))
                .collect(Collectors.toSet());
        Set<String> newApps= applications.stream()
                .map(a -> a.name)
                .collect(Collectors.toSet());
        
        for (String applicationName : newApps) {
            Application application = applicationRepository.findByNameIgnoreCase(applicationName);
            if (application == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application " + applicationName + " is not found in Fasit");
            }
            if (appsInEnvironmentNotInCluster.contains(applicationName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application " + applicationName + " is already mapped to another cluster in " + environment.getName());
            }
            if(!appsInCluster.contains(applicationName)){
                log.debug("Adding new application {} to cluster {} in {}", applicationName, cluster.getName(), environment.getName());
                cluster.addApplication(application);
            }
        }
        // Remove apps not mapped
        
        Set<ApplicationInstance> removedApps = cluster.getApplicationInstances().stream()
            .filter(ai -> !newApps.contains(ai.getApplication().getName()))
            .collect(Collectors.toSet());
        for (ApplicationInstance applicationInstance : removedApps) {
            log.debug("Removing application {} from cluster {} in {}", applicationInstance.getName(), cluster.getName(), environment.getName());
            cluster.removeApplicationByApplication(applicationInstance);
            
        }
        
    }

    private void updateNodeMapping(Set<Link> nodes, Environment environment, Cluster cluster) {
        
        Set<String> nodesInCluster = cluster.getNodes().stream()
            .map(n -> n.getHostname())
            .collect(Collectors.toSet());
        
        Set<String> newNodes= nodes.stream()
                .map(nodelink -> nodelink.name.toLowerCase())
                .collect(Collectors.toSet());

        
        for (String hostname : newNodes) {
            Optional<Node> nodeInEnvironment = environment.getNodes().stream().filter(n -> n.getHostname().equalsIgnoreCase(hostname)).findFirst();
            if (!nodeInEnvironment.isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host " + hostname + " is not in environment " + environment.getName());
            }
            if(!nodesInCluster.contains(hostname)){
                log.debug("Adding new node {} to cluster {} in {}", hostname, cluster.getName(), environment.getName());
                environment.addNode(cluster, nodeInEnvironment.get());
            }
        }
        
        Set<Node> removedNodes = cluster.getNodes().stream()
                .filter(n -> !newNodes.contains(n.getHostname().toLowerCase()))
                .collect(Collectors.toSet());
            for (Node removedNode : removedNodes) {
                log.debug("Removing node {} from cluster {} in {}", removedNode.getHostname(), cluster.getName(), environment.getName());
                cluster.removeNode(removedNode);
                
            }

    }

}