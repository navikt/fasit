package no.nav.aura.envconfig.client;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "applicationGroups")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationGroupListDO {

    @XmlElement(name = "applicationGroup")
	private List<ApplicationGroupDO> applicationGroups;
    
    public ApplicationGroupListDO() {
	}

	public ApplicationGroupListDO(List<ApplicationGroupDO> applicationGroups) {
		this.applicationGroups = applicationGroups;
	}

	public List<ApplicationGroupDO> getApplicationGroups() {
		return applicationGroups;
	}

	public void setApplicationGroups(List<ApplicationGroupDO> applicationGroups) {
		this.applicationGroups = applicationGroups;
	}
}

