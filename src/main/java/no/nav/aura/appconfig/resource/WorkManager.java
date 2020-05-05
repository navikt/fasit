package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class WorkManager extends AbstractJndiCapableResource {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "workTimeout", required = false)
    private int workTimeout = 0;

    @XmlAttribute(name = "alarmThreads", required = false)
    private int alarmThreads = 2;

    @XmlAttribute(name = "maximumThreads", required = false)
    private int maximumThreads = 2;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWorkTimeout() {
        return workTimeout;
    }

    public void setWorkTimeout(int workTimeout) {
        this.workTimeout = workTimeout;
    }

    public int getAlarmThreads() {
        return alarmThreads;
    }

    public void setAlarmThreads(int alarmThreads) {
        this.alarmThreads = alarmThreads;
    }

    public int getMaximumThreads() {
        return maximumThreads;
    }

    public void setMaximumThreads(int maximumThreads) {
        this.maximumThreads = maximumThreads;
    }
}
