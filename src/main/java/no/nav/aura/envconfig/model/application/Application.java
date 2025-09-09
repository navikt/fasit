package no.nav.aura.envconfig.model.application;

import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
@Entity
@Audited
public class Application extends DeleteableEntity implements AccessControlled {

    @Column(name = "app_name", unique = true)
    private String name;
    private String artifactId;
    private String groupId;

    // used by resource scope
    @OneToMany(cascade = CascadeType.MERGE)
    @JoinColumn(name = "application_entid")
    private Set<Resource> resources;

    @Column(name = "port_offset")
    private int portOffset = 0;

    private String onePagerUrl;

    @Embedded
    private AccessControl accessControl;

    // Must be public because of ModelEntityIdentifier reflection
    public Application() {
        this("");
    }


    public Application(String name) {
        this(name, null, null);
    }

    public Application(String name, String artifactId, String groupId) {
        this.name = name;
        this.artifactId = artifactId;
        this.groupId = groupId;
        // hardcoding access to t for edit
        this.accessControl = new AccessControl(EnvironmentClass.t);
    }

    public Application(Application orig) {
        super(orig);
        this.name = orig.name;
        this.artifactId = orig.artifactId;
        this.groupId = orig.groupId;
        this.accessControl = orig.accessControl;
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String toString() {
        return name;
    }

    /** hashcode based on globally unique name */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).build();
    }

    /** equals based on globally unique name */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Application other)) {
            return false;
        }
        return new EqualsBuilder().append(name, other.name).build();
    }

    @Override
    public Map<String, Object> getEnityProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("groupdId", groupId);
        properties.put("artifactId", artifactId);
        properties.put("portOffset", String.valueOf(portOffset));
        return properties;
    }

    public int getPortOffset() {
        return portOffset;
    }

    public void setPortOffset(int portOffset) {
        this.portOffset = portOffset;
    }

    public String getOnePagerUrl() {
        return onePagerUrl;
    }

    public void setOnePagerUrl(String onePagerUrl) {
        this.onePagerUrl = onePagerUrl;
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }

}
