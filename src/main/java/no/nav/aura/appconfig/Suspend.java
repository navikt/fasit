package no.nav.aura.appconfig;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Suspend {

    @XmlAttribute(required = true)
    private String url;

    @XmlAttribute
    private Integer timeoutSeconds;

    @XmlAttribute(required = true)
    private String credential;

    public String getUrl() {
        return url;
    }


    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getCredential() {
        return credential;
    }
}
