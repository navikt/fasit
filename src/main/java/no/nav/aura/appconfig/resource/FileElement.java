package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Files to be copied to server
 */
@XmlRootElement(name = "file")
@XmlAccessorType(XmlAccessType.FIELD)
public class FileElement {

    /** local path to file or folder */
    @XmlAttribute
    private String source;
    /** Name of system property to represent the path of this file */
    @XmlAttribute
    private String mapToProperty;

    public FileElement() {

    }

    public FileElement(String source, String mapToProperty) {
        this.source = source;
        this.mapToProperty = mapToProperty;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMapToProperty() {
        return mapToProperty;
    }

    public void setMapToProperty(String mapToProperty) {
        this.mapToProperty = mapToProperty;
    }

}
