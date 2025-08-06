package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "timerManager")
@XmlAccessorType(XmlAccessType.FIELD)
public class TimerManager extends AbstractJndiCapableResource {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "timerThreads", required = false)
    private int timerThreads = 2;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTimerThreads() {
        return timerThreads;
    }

    public void setTimerThreads(int timerThreads) {
        this.timerThreads = timerThreads;
    }
}
