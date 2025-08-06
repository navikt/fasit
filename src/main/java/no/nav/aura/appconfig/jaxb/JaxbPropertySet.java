package no.nav.aura.appconfig.jaxb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "customProperties")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxbPropertySet {
    @XmlElementRef
    List<JaxbProperty> properties = new ArrayList<>();

    @XmlAttribute(required = true, name = "parentObject")
    String parentObject = ParentConfigObject.J2EEResourceProperty.name();

    public JaxbPropertySet() {
    }

    public JaxbPropertySet(String parentObject, List<JaxbProperty> properties) {
        this.parentObject = parentObject;
        this.properties = properties;
    }

    public String getParentObject() {
        return parentObject;
    }

    public Map<String, String> getProperties() {
        Map<String, String> customProperties = new HashMap<>();
        for (JaxbProperty property : properties) {
            customProperties.put(property.getKey(), property.getValue());
        }
        return customProperties;
    }
}
