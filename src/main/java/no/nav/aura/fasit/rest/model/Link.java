package no.nav.aura.fasit.rest.model;

import java.net.URI;

import javax.validation.constraints.NotNull;

public class Link {
    
    @NotNull(message="name is required")
    public String name;
    public String id;
    public URI ref;
    
    public Link() {
    }
    
    public Link(String name) {
        this.name = name;
    }
    public Link(String name, URI ref) {
        this.name = name;
        this.ref = ref;
    }

    public Link withId(Long id) {
        this.id = String.valueOf(id);
        return this;
    }
    
    @Override
    public String toString() {
        return name + ":" + ref;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Link other = (Link) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public String getName() {
        return name;
    }

    public URI getRef() {
        return ref;
    }
    
    

}
