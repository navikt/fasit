package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Basepart of an url (Protocol, hostname and port) 
 * Like http://myserver:8080
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FileLibrary extends AbstractPropertyResource {
    
    public FileLibrary() {
    }
    
    public FileLibrary(String alias, String mapToProperty){
        setAlias(alias);
        setMapToProperty(mapToProperty);
    }
}
