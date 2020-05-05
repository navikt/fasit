package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * SMTP server information
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SmtpServer extends AbstractJndiCapableResource {

    @XmlAttribute(required = false)
    private String refCredentialAlias;

    public String getCredentialAlias() {
        return refCredentialAlias;
    }

    public void setCredentialAlias(String credentialAlias) {
        this.refCredentialAlias = credentialAlias;
    }

}
