package no.nav.aura.envconfig.model.infrastructure;

import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import jakarta.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "app_instance_res_refs")
@Audited
@AuditTable("app_inst_res_refs_aud")
public class ResourceReference extends ModelEntity implements Reference {

    @ManyToOne(optional = true, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "resource_entid")
    private Resource resource;

    private String alias;
    
    private Boolean future;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    private Long revision;

    @SuppressWarnings("unused")
    private ResourceReference() {
    }

    public ResourceReference(Resource resource, Long revision) {
        this(resource, resource.getAlias(), resource.getType(), revision, false);
    }

    private ResourceReference(Resource resource, String alias, ResourceType resourceType, Long revision, boolean future) {
        this.resource = resource;
        this.alias = alias;
        this.resourceType = resourceType;
        this.revision = revision;
        this.future = future;
    }

    public static ResourceReference future(String alias, ResourceType resourceType) {
        return new ResourceReference(null, alias, resourceType, null, true);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.future = false;
        this.resource = resource;
    }

    @Override
    public String getName() {
        return alias;
    }

    public Boolean isFuture() {
        return future;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(alias).append(future).append(resourceType).build();
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof ResourceReference)) {
            return false;
        }
        ResourceReference other = (ResourceReference) obj;
        return new EqualsBuilder().append(alias, other.alias).append(future, other.future).append(resourceType, other.resourceType).build();
    }
}
