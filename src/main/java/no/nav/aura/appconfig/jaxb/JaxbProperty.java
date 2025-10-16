package no.nav.aura.appconfig.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "property")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxbProperty {

    @XmlAttribute
    private String key;
    @XmlAttribute
    private String value;

    public JaxbProperty() {
    }

    public JaxbProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
