package no.nav.aura.appconfig.exposed;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ExposedQueue extends ExposedService {
    
    public ExposedQueue() {
    }
    
    public ExposedQueue(String name){
     this.setName(name);  
    }


}