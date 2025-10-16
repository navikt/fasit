package no.nav.aura.envconfig.client.rest;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@XmlRootElement(name = "collection")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceElementList {

	@XmlElement(name = "resource")
	private List<ResourceElement> resourceElements = new ArrayList<>();

  	public ResourceElementList() {
	}

    public ResourceElementList(ResourceElement[] resources) {
        if (resources != null) {
            this.resourceElements.addAll(Arrays.asList(resources));
        }
    }
    
    public ResourceElementList(Collection<ResourceElement> resources) {
        if (resources != null) {
            this.resourceElements.addAll(resources);
        }
    }

	public List<ResourceElement> getResourceElements() {
		return resourceElements;
	}

	public void setResourceElements(List<ResourceElement> resourceElements) {
		this.resourceElements = resourceElements;
	}
	
	public void add(ResourceElement resourceElement) {
		this.resourceElements.add(resourceElement);
	}
	
	public int size() {
		return resourceElements.size();
	}
	
	public boolean isEmpty() {
		return resourceElements.isEmpty();
	}
}
