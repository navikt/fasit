package no.nav.aura.envconfig.client;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "environments")
@XmlAccessorType(XmlAccessType.FIELD)
public class EnvironmentListDO {
	
    @XmlElement(name = "environment")
	private List<EnvironmentDO> environments;

	public EnvironmentListDO() {
	}

	public EnvironmentListDO(List<EnvironmentDO> environments) {
		super();
		this.environments = environments;
	}

	public List<EnvironmentDO> getEnvironments() {
		return environments;
	}

	public void setEnvironments(List<EnvironmentDO> environments) {
		this.environments = environments;
	}

}
