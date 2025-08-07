package no.nav.aura.envconfig.client;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "applications")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationInstanceListDO {
    @XmlElement(name = "application")
    private List<ApplicationInstanceDO> applications;

    public ApplicationInstanceListDO() {
	}
    
	public ApplicationInstanceListDO(List<ApplicationInstanceDO> applications) {
		this.applications = applications;
	}

	public List<ApplicationInstanceDO> getApplications() {
		return applications;
	}

	public void setApplications(List<ApplicationInstanceDO> applications) {
		this.applications = applications;
	}
    
}

