package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

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
