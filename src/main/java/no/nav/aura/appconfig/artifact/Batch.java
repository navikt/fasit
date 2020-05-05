package no.nav.aura.appconfig.artifact;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Batch extends Artifact {

    /**
     * Map this directory to a given path on the target system. The sysmlink must be absolute path. A symlink can not be the
     * parent of another symlink
     */
    @XmlAttribute(required = false)
    private String symlink;

    public Batch() {
    }

    public Batch(String groupId, String artifactId, String version, String symlink) {
        setGroupId(groupId);
        setArtifactId(artifactId);
        setVersion(version);
        setSymlink(symlink);
    }

    public void setSymlink(String symlink) {
        this.symlink = symlink;
    }

    public String getSymlink() {
        return symlink;
    }
}
