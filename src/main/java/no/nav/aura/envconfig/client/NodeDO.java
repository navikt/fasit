package no.nav.aura.envconfig.client;

import java.net.URI;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "node")
@XmlAccessorType(XmlAccessType.FIELD)
public class NodeDO {

    @XmlAttribute
    private URI ref;

    @XmlID
    private String hostname;
    private String ipAddress;
    private String environmentName;
    private String environmentClass;
    private String applicationMappingName;
    private String[] applicationName;

    private String username;
    private String name;
    private String zone;
    private String domain;
    private URI passwordRef;
    private String password;
    private PlatformTypeDO platformType;
    private LifeCycleStatusDO status;
    private String accessAdGroup;
    
    public NodeDO() {
    }
    
    public NodeDO(String hostname, PlatformTypeDO type){
        this.hostname = hostname;
    }

    // 20.3 2014
    @Deprecated
    private int httpsPort;

    public String getHostname() {
        return hostname;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public DomainDO getDomain() {
        return DomainDO.fromFqdn(domain);
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String toString() {
        return hostname;
    }

    public void setPasswordRef(URI passwordRef) {
        this.passwordRef = passwordRef;
    }

    public URI getPasswordRef() {
        return passwordRef;
    }

    public PlatformTypeDO getPlatformType() {
        return platformType;
    }

    public void setPlatformType(PlatformTypeDO platformType) {
        this.platformType = platformType;
    }

    public String getShortName() {
        return hostname.split("\\.")[0];
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

    public String getApplicationMappingName() {
        return applicationMappingName;
    }

    public void setApplicationMappingName(String applicationMappingName) {
        this.applicationMappingName = applicationMappingName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // 20.3 2014
    @Deprecated
    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    // 20.3 2014
    @Deprecated
    public int getHttpsPort() {
        return httpsPort;
    }

    public URI getRef() {
        return ref;
    }

    public void setRef(URI ref) {
        this.ref = ref;
    }

    public LifeCycleStatusDO getStatus() {
        return status;
    }

    public void setStatus(LifeCycleStatusDO status) {
        this.status = status;
    }

    public String[] getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String[] applicationName) {
        this.applicationName = applicationName;
    }

    public String getAccessAdGroup() {
        return accessAdGroup;
    }

    public void setAccessAdGroup(String accessGroup) {
        this.accessAdGroup = accessGroup;
    }
}
