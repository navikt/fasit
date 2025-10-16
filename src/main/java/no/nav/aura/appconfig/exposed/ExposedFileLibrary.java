package no.nav.aura.appconfig.exposed;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * directory exposed by one application and used by another on the same node. Feks ledetekster
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ExposedFileLibrary extends ExposedService {
    
    // name of the directory to be exposed
    @XmlAttribute
    private String directory;
    
    public ExposedFileLibrary() {
    }
    
    public ExposedFileLibrary(String name, String directory){
        this.directory = directory;
        this.setName(name);  
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }



}