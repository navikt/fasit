package no.nav.aura.envconfig.model.infrastructure;

import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.Scopeable;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.resource.Scope;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.envers.Audited;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
@Entity
@Audited
public class Environment extends DeleteableEntity implements Scopeable, AccessControlled {

    @Column(unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private EnvironmentClass envClass;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "env_id")
    private Set<Cluster> clusters = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "env_id")
    private Set<Node> nodes = new HashSet<>();

    @Embedded
    private AccessControl accessControl;

    private static final Logger log = LoggerFactory.getLogger(Environment.class);

    @SuppressWarnings("unused")
    public Environment() {
    }


    public Environment(Environment orig) {
        super(orig);
        this.name = orig.name;
        this.envClass = orig.envClass;
        this.clusters = new HashSet<>(orig.clusters);
        this.nodes = new HashSet<>(orig.nodes);
        this.accessControl = orig.accessControl;
    }

    public Environment(String envName, EnvironmentClass envClass) {
        this.name = envName.toLowerCase();
        this.envClass = envClass;
        this.accessControl = new AccessControl(envClass);
    }

    public String getName() {
        return name;
    }

    public EnvironmentClass getEnvClass() {
        return envClass;
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }

    /**
     * RULE There can only be one instance of an application in an environment
     */
    @Nullable
    public ApplicationInstance findApplicationByName(String appName) {
        ApplicationInstance returnInstance = null;
        for (ApplicationInstance instance : getApplicationInstances()) {
            if (appName.toLowerCase().equals(instance.getApplication().getName().toLowerCase())) {
                return instance;
            }
        }
        return returnInstance;
    }

    public Set<Cluster> getClusters() {
        return new HashSet<>(clusters);
    }

    public <T extends Cluster> T addCluster(T cluster) {
        clusters.add(cluster);
        cluster.setEnvironment(this);
        return cluster;
    }

    public Set<Node> getNodes() {
        return new HashSet<>(nodes);
    }

    public Node addNode(Cluster cluster, Node node) {
        addNode(node);
        cluster.addNode(node);
        cluster.setEnvironment(this);
        return node;
    }

    public Node addNode(Node node) {
        nodes.add(node);
        node.setEnvironment(this);
        return node;
    }

    public void removeNode(Node node) {
        for (Cluster cluster : clusters) {
        	if(cluster.getNodes().contains(node)) {
        		log.debug("Removing node {} from cluster {}", node.getHostname(), cluster.getName());
        		cluster.removeNode(node);
        	}
        }
        this.nodes.remove(node);
        log.debug("Removed node {} from environment {}", node.getHostname(), name);

    }

    @Nullable
    public Cluster findClusterByName(String clusterName) {
        for (Cluster cluster : clusters) {
            if (cluster.getName().equalsIgnoreCase(clusterName)) {
                return cluster;
            }
        }
        return null;
    }

    @Transactional
    public Set<ApplicationInstance> getApplicationInstances() {
        Set<ApplicationInstance> apps = new HashSet<>();
        for (Cluster cluster : getClusters()) {
            apps.addAll(cluster.getApplicationInstances());
        }

        return apps;
    }

    public Set<Application> getApplications() {
        Set<Application> apps = new HashSet<>();
        for (ApplicationInstance instance : getApplicationInstances()) {
            apps.add(instance.getApplication());
        }
        return apps;
    }

    @Override
    public int hashCode() {
        return createHashCodeBuilder().append(name).build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Environment)) {
            return false;
        }
        Environment other = (Environment) obj;
        return createEqualsBuilder(other).append(name, other.name).build();
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this).append("name", name).append("environmentClass", envClass).append("clusters", clusters.size()).append("nodes", nodes.size());
        return builder.toString();
    }

    public Scope getScope() {
        return new Scope(envClass).envName(name);
    }

    public void removeCluster(Cluster cluster) {
        if (!clusters.remove(cluster)) {        	
            throw new RuntimeException("Cluster " + cluster + " to delete from environment " + name + " not found");
        }
        cluster.removeEnvironment();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnvClass(EnvironmentClass envClass) {
        this.envClass = envClass;
    }

}
