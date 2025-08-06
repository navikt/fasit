package no.nav.aura.envconfig.client;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "applications")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationListDO {
	
    @XmlElement(name = "application")
	private List<ApplicationDO> applications;

	public ApplicationListDO() {
	}

	public ApplicationListDO(List<ApplicationDO> applications) {
		super();
		this.applications = applications;
	}

	public List<ApplicationDO> getApplications() {
		return applications;
	}

	public void setApplications(List<ApplicationDO> applications) {
		this.applications = applications;
	}

}
