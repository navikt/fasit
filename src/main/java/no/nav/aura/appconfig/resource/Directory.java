package no.nav.aura.appconfig.resource;

import no.nav.aura.appconfig.Namespaces;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Local files on the target server. This resource will create a directory on each node the application is deployed to.
 */
@XmlRootElement(name = "directory")
@XmlAccessorType(XmlAccessType.FIELD)
public class Directory extends Resource {

    /** name of the folder on the filesystem. NB not the full path */
    @XmlAttribute(required = true)
    private String name;

    /** Name of the property this resource is mapped to. This property will contain the full path to the folder */
    @XmlAttribute(required = false)
    private String mapToProperty;

    /**
     * Map this directory to a given path on the target system. The sysmlink must be absolute path. A symlink can not be the
     * parent of another symlink
     */
    @XmlAttribute(required = false)
    private String symlink;

    /** Should the folder be deleted on application redeploy, default is false */
    @XmlAttribute
    private boolean temporary;

    /** Files to to be copied to this folder */
    @XmlElement(name = "file", namespace = Namespaces.DEFAULT)
    private List<FileElement> files = new ArrayList<>();

    /** Mount this folder on a NFS share. If no NFS is availible this will be a local share */
    @XmlElement(name = "mountOnNfs", namespace = Namespaces.DEFAULT)
    private NfsMount mountOnNfs;
    /** Mount this folder on a NFS share. If no NFS is availible this will be a local share */
    @XmlElement(name = "mountOnCifs", namespace = Namespaces.DEFAULT)
    private CifsMount mountOnCifs;

    public Directory() {
    }

    public Directory(String name, String mapToProperty, boolean temporary) {
        this.name = name;
        this.mapToProperty = mapToProperty;
        this.temporary = temporary;
    }

    /**
     * Name of the property this resource is mapped to at the target system. This property will contain the full path to the
     * folder
     */
    public String getMapToProperty() {
        return mapToProperty;
    }

    /** Name of the property this resource is mapped to at the target system */
    public void setMapToProperty(String mapToProperty) {
        this.mapToProperty = mapToProperty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Should the folder be deleted on application restart */
    public boolean isTemporary() {
        return temporary;
    }

    /** Should the folder be deleted on application redeploy, default is false */
    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public List<FileElement> getFiles() {
        return files;
    }

    public void setFiles(List<FileElement> files) {
        this.files = files;
    }

    public String getSymlink() {
        return symlink;
    }

    public void setSymlink(String symlink) {
        this.symlink = symlink;
    }

    public NfsMount getMountOnNfs() {
        return mountOnNfs;
    }

    public void setMountOnNfs(NfsMount mountOnNfs) {
        this.mountOnNfs = mountOnNfs;
    }

    public CifsMount getMountOnCifs() {
        return mountOnCifs;
    }

    public void setMountOnCifs(CifsMount mountOnCifs) {
        this.mountOnCifs = mountOnCifs;
    }

}
