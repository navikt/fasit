package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

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
