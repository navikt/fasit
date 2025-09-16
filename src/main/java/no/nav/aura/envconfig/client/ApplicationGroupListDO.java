package no.nav.aura.envconfig.client;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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

