package no.nav.aura.envconfig.model.infrastructure;

import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.secrets.Secret;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
@Entity
@Audited
public class Node extends DeleteableEntity implements EnvironmentDependant, AccessControlled {

    private String hostname;

    private String username;

    @Enumerated(EnumType.STRING)
    private PlatformType platformType;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "password_entid")
    private Secret password;

    @ManyToMany(mappedBy="nodes" , cascade = { CascadeType.MERGE, CascadeType.PERSIST })
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
        Pattern domainPattern = Pattern.compile("[\\w-]*\\.\\w*$");
        Matcher domainMatcher = domainPattern.matcher(hostname);
        if (!domainMatcher.find()) {
            throw new IllegalArgumentException("Unable to determine domain for hostname " + hostname);
        }
        return Domain.fromFqdn(domainMatcher.group());
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

}
