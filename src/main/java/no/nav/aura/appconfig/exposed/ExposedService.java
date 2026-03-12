package no.nav.aura.appconfig.exposed;

import no.nav.aura.appconfig.Namespaces;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({ExposedSoap.class, ExposedQueue.class, ExposedEjb.class, ExposedRest.class, ExposedUrl.class, ExposedLetterTemplate.class, ExposedQueue.class, ExposedFileLibrary.class})
public abstract class ExposedService {

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String description;

    @XmlElement(name = "exportToZone", namespace = Namespaces.DEFAULT)
    private List<NetworkZone> exportToZones = new ArrayList<NetworkZone>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<NetworkZone> getExportToZones() {
        return exportToZones;
    }

    public void setExportToZones(List<NetworkZone> zones) {
        this.exportToZones = zones;
    }

    public boolean exportTo(NetworkZone zone) {
        return exportToZones.contains(zone);
    }

}
