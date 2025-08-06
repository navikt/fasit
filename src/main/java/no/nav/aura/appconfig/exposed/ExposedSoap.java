package no.nav.aura.appconfig.exposed;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ExposedSoap extends ExposedService {

	@XmlAttribute
	private String type;

	@XmlAttribute
	private String path;

	@XmlAttribute
	private String wsdlGroupId;

	@XmlAttribute
	private String wsdlArtifactId;

	@XmlAttribute
	private String wsdlVersion;

	@XmlAttribute
	private boolean deployToServiceGateway = true;

	@XmlAttribute
	private SecurityToken securityToken;


	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getWsdlGroupId() {
		return wsdlGroupId;
	}

	public void setWsdlGroupId(String wsdlGroupId) {
		this.wsdlGroupId = wsdlGroupId;
	}

	public String getWsdlArtifactId() {
		return wsdlArtifactId;
	}

	public void setWsdlArtifactId(String wsdlArtifactId) {
		this.wsdlArtifactId = wsdlArtifactId;
	}

	public String getWsdlVersion() {
		return wsdlVersion;
	}

	public void setWsdlVersion(String wsdlVersion) {
		this.wsdlVersion = wsdlVersion;
	}


	public SecurityToken getSecurityToken() {
		return securityToken;
	}

	public void setSecurityToken(SecurityToken securityToken) {
		this.securityToken = securityToken;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isDeployToServiceGateway() {
		return deployToServiceGateway;
	}

	public void setDeployToServiceGateway(boolean deployToServiceGateway) {
		this.deployToServiceGateway = deployToServiceGateway;
	}
}
