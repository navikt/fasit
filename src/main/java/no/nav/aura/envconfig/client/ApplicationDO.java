package no.nav.aura.envconfig.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "application")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ApplicationDO {
    private String name;
    private String appConfigGroupId;
    private String appConfigArtifactId;
    private ApplicationGroupDO applicationGroup;

    private int portOffset;

    public ApplicationDO() {
    }

    public ApplicationDO(String name, String appConfigGroupId, String appConfigArtifactId) {
        this(name, appConfigGroupId, appConfigArtifactId, 0);
    }

    public ApplicationDO(String name, String appConfigGroupId, String appConfigArtifactId, int portOffset) {
        this.name = name;
        this.appConfigGroupId = appConfigGroupId;
        this.appConfigArtifactId = appConfigArtifactId;
        this.portOffset = portOffset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppConfigGroupId() {
        return appConfigGroupId;
    }

    public void setAppConfigGroupId(String appConfigGroupId) {
        this.appConfigGroupId = appConfigGroupId;
    }

    public String getAppConfigArtifactId() {
        return appConfigArtifactId;
    }

    public void setAppConfigArtifactId(String appConfigArtifactId) {
        this.appConfigArtifactId = appConfigArtifactId;
    }

    public int getPortOffset() {
        return portOffset;
    }

    public void setPortOffset(int portOffset) {
        this.portOffset = portOffset;
    }

    public void setApplicationGroup(ApplicationGroupDO applicationGroup) {
        this.applicationGroup = applicationGroup;
    }

    public ApplicationGroupDO getApplicationGroup() {
        return applicationGroup;
    }
}
