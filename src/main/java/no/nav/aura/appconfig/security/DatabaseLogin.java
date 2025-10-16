package no.nav.aura.appconfig.security;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DatabaseLogin extends Login {

    @XmlAttribute(required = true)
    private String domainName;

    @XmlAttribute(required = true)
    private String dsJndiName;

    @XmlAttribute(required = true)
    private String principalsQuery;

    @XmlAttribute(required = true)
    private String rolesQuery;

    @XmlAttribute
    private String hashAlgorithm = "SHA-256";

    @XmlAttribute
    private String hashEncoding = "base64";

    public String getDomainName() {
        return domainName;
    }

    public String getDsJndiName() {
        return dsJndiName;
    }

    public String getPrincipalsQuery() {
        return principalsQuery;
    }

    public String getRolesQuery() {
        return rolesQuery;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public String getHashEncoding() {
        return hashEncoding;
    }

    public void setHashEncoding(String hashEncoding) {
        this.hashEncoding= hashEncoding;
    }
}
