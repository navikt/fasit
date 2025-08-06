package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class NfsMount {

    @XmlAttribute(required = false)
    private String customNFSResourceAlias;

    public NfsMount() {
    }

    public NfsMount(String customNFSResourceAlias) {
        this.customNFSResourceAlias = customNFSResourceAlias;
    }

    public String getCustomNFSResourceAlias() {
        return customNFSResourceAlias;
    }
}
