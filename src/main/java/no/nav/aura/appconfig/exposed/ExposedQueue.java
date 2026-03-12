package no.nav.aura.appconfig.exposed;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ExposedQueue extends ExposedService {
    
    public ExposedQueue() {
    }
    
    public ExposedQueue(String name){
     this.setName(name);  
    }


}