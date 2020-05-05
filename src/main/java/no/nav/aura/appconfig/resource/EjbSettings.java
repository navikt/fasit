package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class EjbSettings {

    @XmlAttribute(name = "sessionBeanPoolSize")
    private int sessionBeanPoolSize = 20;

    @XmlAttribute(name = "sessionBeanTimeout")
    private int sessionBeanTimeout = 5;

    @XmlAttribute(name = "mdbPoolSize")
    private int mdbPoolSize = 20;

    @XmlAttribute(name = "mdbTimeout")
    private int mdbTimeout = 5;

    public int getSessionBeanPoolSize() { return sessionBeanPoolSize; }

    public int getSessionBeanTimeout() { return sessionBeanTimeout; }

    public int getMdbPoolSize() { return mdbPoolSize; }

    public int getMdbTimeout() { return mdbTimeout; }
}
