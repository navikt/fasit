package no.nav.aura.envconfig.model.infrastructure;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.*;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("serial")
@Entity
@Table(name = "clusters")
@Audited
public class Cluster extends DeleteableEntity implements AccessControlled, EnvironmentDependant {

    @Column(name = "cluster_name")
    private String name;

    @Enumerated(EnumType.STRING)
    private Domain domain;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @JoinTable(name = "clusters_node", joinColumns = @JoinColumn(name = "clusters_entid"), inverseJoinColumns = { @JoinColumn(name = "nodes_entid") })
    private List<Node> nodes = new ArrayList<Node>();

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cluster_entid")
    @AuditJoinTable(name = "cluster_appinst_aud")
    private Set<ApplicationInstance> applications = new HashSet<ApplicationInstance>();

    private String loadBalancerUrl;

    @Embedded
    private AccessControl accessControl;

    Cluster() {
    }


    public Cluster(String name, Domain domain) {
        this.name = name;
        this.domain = domain;
        this.accessControl = new AccessControl(domain.getEnvironmentClass());
    }

    @Override
    public Map<String, Object> getEnityProperties() {
        Map<String, Object> properties = new HashMap();
        properties.put("zone", domain.getZone());
        properties.put("nodes", nodes.stream().map(n -> n.getHostname()).collect(toList()));
        properties.put("applications", applications.stream().map(a -> a.getName()).collect(toList()));
        properties.put("loadbalancerUrl", loadBalancerUrl);
        return properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Node> getNodes() {
        return Sets.newHashSet(nodes);
    }

    public void addNode(Node node) {
        nodes.add(node);

    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public Set<ApplicationInstance> getApplicationInstances() {
        return Sets.newHashSet(applications);
    }

    public ApplicationInstance addApplication(Application app) {
        if (getApplications().contains(app)) {
            throw new IllegalArgumentException("Application " + app.getName() + " is already registered in cluster " + this);
        }
        ApplicationInstance appInstance = new ApplicationInstance(app, this);
        applications.add(appInstance);
        return appInstance;
    }

    public Collection<Application> getApplications() {
        Set<Application> apps = new HashSet<>();
        for (ApplicationInstance appInstance : applications) {
            apps.add(appInstance.getApplication());
        }
        return apps;
    }

    public void removeNode(Node node) {
        nodes.remove(node);
    }

    public String getLoadBalancerUrl() {
        return loadBalancerUrl;
    }

    public int getHttpsPortFromPlatformType() {
        return getPlatformType().getBaseHttpsPort();
    }

    public int getBaseBootstrapPortFromPlatformType() {
        return getPlatformType().getBaseBootstrapPort();
    }

    private PlatformType getPlatformType() {
        return getNodes().isEmpty() ? PlatformType.WILDFLY : getNodes().iterator().next().getPlatformType();
    }

    public void setLoadBalancerUrl(String url) {
        if (url != null && url != "") {
            if (url.matches("^https?://([-\\w\\.]+)+(:\\d+)?(/.*)?"))
                this.loadBalancerUrl = url;
            else
                throw new RuntimeException("Invalid URL format, " + url);
        }
    }

    public void removeNodeById(final Long id) {
        boolean removed = Iterables.removeIf(nodes, node -> node.getID().equals(id));
        if (!removed) {
            throw new RuntimeException("Unable to remove node " + id + " from cluster " + toString());
        }
    }

    public void clearApplicationInstances() {
        applications.clear();
    }

    public boolean removeApplicationByApplication(ApplicationInstance applicationInstance) {
       return applications.remove(applicationInstance);
    }

    public void removeApplicationByApplicationId(final Long id) {
        boolean removed = Iterables.removeIf(applications, applicationInstance -> applicationInstance.getApplication().getID().equals(id));
        if (!removed) {
            throw new RuntimeException("Unable to remove application " + id + " from cluster " + toString());
        }
    }

    public void addApplicationGroup(ApplicationGroup applicationGroup) {
        for (Application application : applicationGroup.getApplications()) {
            addApplication(application);
        }
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }
}
