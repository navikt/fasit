package no.nav.aura.envconfig.client.rest;

import static no.nav.aura.envconfig.client.rest.PropertyElement.Type.STRING;

import java.net.URI;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "property")
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyElement {

    public enum Type {
        STRING, SECRET, FILE
    }

    @XmlAttribute
    private String name;
    @XmlAttribute
    private Type type = STRING;
    private String value;
    private URI ref;

    public PropertyElement() {
    }

    public PropertyElement(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public PropertyElement(String name, URI ref, Type type) {
        this.type = type;
        this.name = name;
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public URI getRef() {
        return ref;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "PropertyElement [name=" + name + ", type=" + type + ", value=" + value + ", ref=" + ref + "]";
    }

}
