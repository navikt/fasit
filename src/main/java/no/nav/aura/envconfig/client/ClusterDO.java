package no.nav.aura.envconfig.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.beans.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Cluster representation for cluster
 */
@XmlRootElement(name = "cluster")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ClusterDO {

    private String type;
    private String environmentName;
    private String environmentClass;
    private String loadBalancerUrl;
    private String domain;
    private String name;
    private List<NodeDO> nodes = new ArrayList<NodeDO>();
    private Collection<String> applications = new ArrayList<String>();

    public boolean isForApplicationGroup() {
        return applications.size() > 1;
    }

    @Deprecated
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NodeDO[] getNodes() {
        return nodes.toArray(new NodeDO[nodes.size()]);
    }

    public void setNodes(NodeDO[] nodes) {
        this.nodes = Arrays.asList(nodes);
    }

    public void addNode(NodeDO node) {
        nodes.add(node);
    }

    @Transient
    public List<NodeDO> getNodesAsList() {
        return nodes;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getEnvironmentClass() {
        return environmentClass;
    }

    public void setEnvironmentClass(String environmentClass) {
        this.environmentClass = environmentClass;
    }

    @Transient
    public DomainDO getDomainDO() {
        return DomainDO.fromFqdn(domain);
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String fqn) {
        this.domain = fqn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoadBalancerUrl() {
        return loadBalancerUrl;
    }

    public void setLoadBalancerUrl(String loadBalancerUrl) {
        this.loadBalancerUrl = loadBalancerUrl;
    }

    public PlatformTypeDO getPlatformType() {
        if (nodes.isEmpty()) {
            return PlatformTypeDO.WILDFLY;
        }
        return nodes.iterator().next().getPlatformType();
    }

    @XmlElement(name = "application")
    public Collection<String> getApplications() {
        return applications;
    }

    public void setApplications(Collection<String> application) {
        this.applications = application;
    }

    public boolean isDevelopment(){
        return this.environmentClass.equalsIgnoreCase("u");
    }

    public boolean hasNodes(){
        return !this.getNodesAsList().isEmpty();
    }

}
