package no.nav.aura.envconfig.model.infrastructure;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
@Entity
@Audited
public class ApplicationInstance extends DeleteableEntity implements EnvironmentDependant {

    @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @JoinColumn(name = "application_entid")
    private Application application;

    // TODO the road to hell is paved with two-way bindings
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "cluster_entid")
    private Cluster cluster;

    private String version;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "applicationinstance_entid")
    @AuditJoinTable(name = "appinst_resref_aud")
    private Set<ResourceReference> resourceReferences = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "applicationinstance_entid")
    @AuditJoinTable(name = "appinst_expref_aud")
    private Set<ExposedServiceReference> exposedServices = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "appinst_entid", referencedColumnName = "entid", nullable = false)
    @AuditJoinTable(name = "appinst_portref_aud")
    private Set<Port> ports = new HashSet<>();

    private String selftestPagePath;

    private ZonedDateTime deployDate;

    @Lob
    private String appconfigXml;

    @SuppressWarnings("unused")
    private ApplicationInstance() {
    }

        public ApplicationInstance(Application application, Cluster cluster) {
        this.application = application;
        this.cluster = cluster;
    }

    @Override
    public Map<String, Object> getEnityProperties() {
        Map<String, Object> properties = new HashMap();
        properties.put("application", application.getName());
        properties.put("cluster", cluster.getName());
        properties.put("version", version != null ? version : "Not deployed");
        properties.put("ports", ports);
        properties.put("selftestPath", selftestPagePath);

        return properties;
    }

    public boolean isDeployed() {
        return version != null;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Set<ResourceReference> getResourceReferences() {
        return resourceReferences;
    }

    public Set<ExposedServiceReference> getExposedServices() {
        return exposedServices;
    }

    @Override
    public String getName() {
        if (isDeployed()) {
            return application.getName() + ":" + version;
        }
        return application.getName() + ":(not deployed)";
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getSelftestPagePath() {
        return selftestPagePath;
    }

    public void setSelftestPagePath(String selftestPagePath) {
        this.selftestPagePath = selftestPagePath;
    }

    public String getAppconfigXml() {
        return appconfigXml;
    }

    public void setAppconfigXml(String appconfigXml) {
        this.appconfigXml = appconfigXml;
    }

    public int getHttpsPort() {
        return cluster.getHttpsPortFromPlatformType() + application.getPortOffset();
    }

    public Domain getDomain() {
        return cluster.getDomain();
    }

    public ZonedDateTime getDeployDate() {
        return deployDate;
    }

    public void setDeployDate(ZonedDateTime dateTime) {
        this.deployDate = dateTime;
    }

    public void setExposedServices(Set<ExposedServiceReference> exposedServices) {
        this.exposedServices.clear();

        if (exposedServices != null) {
            this.exposedServices.addAll(exposedServices);
        }
    }

    public void setResourceReferences(Set<ResourceReference> resourceReferences) {
        this.resourceReferences = resourceReferences;
    }

    public void setPorts(Set<Port> ports) {
        this.ports.clear();

        if (ports != null) {
            this.ports.addAll(ports);
        }
    }

    public Set<Port> getPorts() {
        return ports;
    }
}
