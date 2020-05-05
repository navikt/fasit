package no.nav.aura.envconfig.model.infrastructure;

import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.resource.Resource;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "appinstance_exposedservices")
@Audited
@AuditTable("appinst_exposedservices_aud")
public class ExposedServiceReference extends ModelEntity implements Reference {

    @ManyToOne(cascade = { CascadeType.ALL })
    @JoinColumn(name = "resource_entid")
    private Resource resource;

    @Column(name = "resource_alias")
    private String resourceAlias;

    private Long revision;

    @SuppressWarnings("unused")
    private ExposedServiceReference() {
    }

    public ExposedServiceReference(Resource resource, Long revision) {
        this.resource = resource;
        if (resource != null) {
            this.resourceAlias = resource.getAlias();
        }
        this.revision = revision;
    }

    @Override
    public String getName() {
        return resourceAlias;
    }

    public String getResourceAlias() {
        return resourceAlias;
    }

    public void setResourceAlias(String resourceAlias) {
        this.resourceAlias = resourceAlias;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
        this.resourceAlias = resource == null ? null : resource.getAlias();
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(resourceAlias).build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExposedServiceReference)) {
            return false;
        }
        ExposedServiceReference other = (ExposedServiceReference) obj;
        return new EqualsBuilder().append(resourceAlias, other.resourceAlias).build();

    }

}
