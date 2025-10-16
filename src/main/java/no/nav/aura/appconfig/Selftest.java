package no.nav.aura.appconfig;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

public class Selftest {

	@XmlElement(name = "path", namespace = Namespaces.DEFAULT)
	private String path;

	@XmlElement(name = "humanReadablePath", namespace = Namespaces.DEFAULT)
	private String humanReadablePath;

	@XmlAttribute
	private int delayInSeconds;

	public String getPath() {
		return path;
	}

	public String getHumanReadablePath() {
		return humanReadablePath;
	}

	public int getDelayInSeconds() { return delayInSeconds;	}
}
