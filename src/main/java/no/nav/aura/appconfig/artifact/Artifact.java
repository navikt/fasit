package no.nav.aura.appconfig.artifact;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;

/**
 * Artifact to install on the plattform. Points to a maven artifact with groupid, artifactid and version. If version is not
 * provided the version of the config artifact is used
 * 
 * startUpOrder is used to determine start order for multiple artifacts. Smallest number starts first. Default value is 10.
 * 
 * displayName is used when an application contains multiple ear files as the named shown in the admin console of the
 * application server
 * 
 */
@XmlSeeAlso({ Ear.class, Batch.class })
@XmlAccessorType(XmlAccessType.FIELD)
public class Artifact {
    @XmlAttribute(required = true)
    private String groupId;
    @XmlAttribute(required = true)
    private String artifactId;
    @XmlAttribute(required = false)
    private String version;

    public Artifact() {}

    public Artifact(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGAV() {
        return groupId + ":" + artifactId + ":" + version;
    }

}
