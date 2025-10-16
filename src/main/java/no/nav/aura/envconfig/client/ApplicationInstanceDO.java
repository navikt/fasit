package no.nav.aura.envconfig.client;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.web.util.UriComponentsBuilder;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import no.nav.aura.envconfig.client.adapter.ZonedDateTimeAdapter;

@XmlRootElement(name = "application")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationInstanceDO {

    @XmlAttribute
    private URI ref;
    private String name;
    private String version;
    
    @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
    private ZonedDateTime lastDeployment;
    private String deployedBy;
    private String selftestPagePath;
    private URI appConfigRef;
    private ClusterDO cluster;
    private int httpsPort;
    private String loadBalancerUrl;
    private String envName;
    private Set<ResourceDO> exposedServices;
    private Set<ResourceDO> usedResources;

    public ApplicationInstanceDO() {
    }

    public ApplicationInstanceDO(String appName, String envName, UriComponentsBuilder uriBuilder) {
        this.ref = uriBuilder.path("environments/{env}/applications/{appname}").build(envName, appName);
        this.name = appName;
        this.envName = envName;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ZonedDateTime getLastDeployment() {
        return lastDeployment;
    }

    public String getDeployedBy() {
        return deployedBy;
    }

    public String getSelftestPagePath() {
        return selftestPagePath;
    }

    public ClusterDO getCluster() {
        return cluster;
    }

    public void setCluster(ClusterDO cluster) {
        this.cluster = cluster;
    }

    public URI getAppConfigRef() {
        return appConfigRef;
    }

    public void setAppConfigRef(URI appConfigRef) {
        this.appConfigRef = appConfigRef;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDeployedBy(String deployedBy) {
        this.deployedBy = deployedBy;
    }

    public void setSelftestPagePath(String selftestPagePath) {
        this.selftestPagePath = selftestPagePath;
    }

    public void setLastDeployment(ZonedDateTime lastDeployment) {
        this.lastDeployment = lastDeployment;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setLoadBalancerUrl(String loadBalancerUrl) {
        this.loadBalancerUrl = loadBalancerUrl;
    }

    public String getLoadBalancerUrl() {
        return loadBalancerUrl;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public Set<ResourceDO> getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(Set<ResourceDO> exposedServices) {
        this.exposedServices = exposedServices;
    }

    public Set<ResourceDO> getUsedResources() {
        return usedResources;
    }

    public void setUsedResources(Set<ResourceDO> usedResources) {
        this.usedResources = usedResources;
    }
}
