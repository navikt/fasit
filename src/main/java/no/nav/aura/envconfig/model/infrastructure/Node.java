package no.nav.aura.envconfig.model.infrastructure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.secrets.Secret;

@SuppressWarnings("serial")
@Entity
@Audited
public class Node extends DeleteableEntity implements EnvironmentDependant, AccessControlled {
	private static final Logger log = LoggerFactory.getLogger(Node.class);
	
    @Column(unique = true)
    private String hostname;

    private String username;

    @ManyToOne
    @JoinColumn(name = "env_id")
    private Environment environment;
    
    @Enumerated(EnumType.STRING)
    private PlatformType platformType;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "password_entid")
    private Secret password;

//    @ManyToMany(mappedBy="nodes" , cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @ManyToMany(mappedBy="nodes")
    private Set<Cluster> clusters = new HashSet<Cluster>();

    @Embedded
    private AccessControl accessControl;

    @SuppressWarnings("unused")
    private Node() {
    }



    public Node(String hostname, String username, String password) {
        this(hostname, username, password, getEnvironmentClass(hostname), PlatformType.WILDFLY);
    }

    public Node(String hostname, String username, String password, EnvironmentClass environmentClass, PlatformType platformType) {
        this.hostname = hostname;
        this.username = username;
        this.password = Secret.withValueAndAuthLevel(password, environmentClass);
        this.platformType = platformType;
        this.accessControl = new AccessControl(environmentClass);
    }

    @Override
    public String getInfo() {
        return platformType.toString();
    }

    @Override
    public Map<String, Object> getEnityProperties() {
        Map<String, Object> properties = new HashMap();
        properties.put("platformType", platformType.toString());

        return properties;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Secret getPassword() {
        return password;
    }

    public void setPassword(Secret password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUserName(String userName) {
        this.username = userName;
    }

    @Override
    public String getName() {
        return hostname;
    }

    public static Domain getDomain(String hostname) {
        int dot = hostname.lastIndexOf('.');
        if (dot < 0 || !allMatch(hostname, dot + 1, hostname.length(), false)) {
            throw new IllegalArgumentException("Unable to determine domain for hostname " + hostname);
        }
        int start = dot;
        while (start > 0 && isDomainChar(hostname.charAt(start - 1), true)) {
            start--;
        }
        return Domain.fromFqdn(hostname.substring(start));
    }

    private static boolean allMatch(String s, int from, int to, boolean allowHyphen) {
        for (int i = from; i < to; i++) {
            if (!isDomainChar(s.charAt(i), allowHyphen)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDomainChar(char c, boolean allowHyphen) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || (allowHyphen && c == '-');
    }

    public Domain getDomain() {
        return getDomain(hostname);
    }

    public static EnvironmentClass getEnvironmentClass(String hostName) {
        Domain domain;
        try {
            domain = Node.getDomain(hostName);
            return domain.getEnvironmentClass();
        } catch (IllegalArgumentException e) {
            return EnvironmentClass.u;
        }
    }

    public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public PlatformType getPlatformType() {
        return platformType != null ? platformType : PlatformType.WILDFLY;
    }

    public void setPlatformType(PlatformType platformType) {
        this.platformType = platformType;
    }

    public Set<Cluster> getClusters() {
        return clusters;
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }
    
    public void addToCluster(Cluster cluster) {
        if (!this.clusters.contains(cluster)) {
            this.clusters.add(cluster);
            if (!cluster.getNodes().contains(this)) {
                cluster.getNodes().add(this);
            }
        }
    }

    public void removeFromCluster(Cluster cluster) {
        if (this.clusters.contains(cluster)) {
        	log.debug("Removing cluster {} from node {}", cluster.getName(), this.getHostname());
            this.clusters.remove(cluster);
        }
    }

}
