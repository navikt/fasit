package no.nav.aura.appconfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import no.nav.aura.appconfig.jaxb.JaxbPropertySet;
import no.nav.aura.appconfig.jaxb.ParentConfigObject;

public class JaxbPropertyHelper {

    public static Map<String, String> getCustomProperties(ParentConfigObject parentObject , List<JaxbPropertySet> customProperties) {
        return getCustomProperties(parentObject.toString(), customProperties);
    }

    public static Map<String, String> getCustomProperties(String parentObject, List<JaxbPropertySet> customProperties) {
        JaxbPropertySet propertySet = getPropertySetForParentObject(parentObject, customProperties);
        return propertySet != null ? propertySet.getProperties() : new HashMap<String, String>();
    }

    public static Map<String, String> getCustomProperties(List<JaxbPropertySet> customProperties) {
        Map<String, String> properties = new HashMap<>();
        for ( JaxbPropertySet propertySet : customProperties) {
            Iterator<Map.Entry<String, String>> iterator = propertySet.getProperties().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> property = iterator.next();
                properties.put(property.getKey(), property.getValue());
            }
        }
        return properties;
    }

    private static JaxbPropertySet getPropertySetForParentObject(String parentObject, List<JaxbPropertySet> customProperties) {
        for (JaxbPropertySet customPropertySet : customProperties) {
            if (customPropertySet.getParentObject().equals(parentObject)) {
                return customPropertySet;
            }
        }
        return null;
    }


}
