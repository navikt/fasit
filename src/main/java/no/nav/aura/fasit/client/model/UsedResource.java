package no.nav.aura.fasit.client.model;

import no.nav.aura.envconfig.client.rest.ResourceElement;

public class UsedResource {

    private long id;
    private long revision;

    public UsedResource() {
    }

    public UsedResource(long id, long revision) {
        this.setId(id);
        this.setRevision(revision);
    }

    public UsedResource(ResourceElement resource) {
        this.setId(resource.getId());
        this.setRevision(resource.getRevision());
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (int) (revision ^ (revision >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UsedResource other = (UsedResource) obj;
        if (id != other.id)
            return false;
        if (revision != other.revision)
            return false;
        return true;
    }

}
