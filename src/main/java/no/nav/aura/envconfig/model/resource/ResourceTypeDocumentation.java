package no.nav.aura.envconfig.model.resource;

public class ResourceTypeDocumentation {

	private String doc;
	private String appConfigLink;

	public ResourceTypeDocumentation(String doc, String appConfigLink){
		this.doc = doc;
		this.appConfigLink = appConfigLink;
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

	public String getAppConfigLink() {
		return appConfigLink;
	}

	public void setAppConfigLink(String appConfigLink) {
		this.appConfigLink = appConfigLink;
	}
}
