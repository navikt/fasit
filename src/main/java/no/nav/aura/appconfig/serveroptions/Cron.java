package no.nav.aura.appconfig.serveroptions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Cron {

	public Cron() {
	}

	public Cron(String description, String schedule, String command) {
		this.description = description;
		this.schedule = schedule;
		this.command = command;
	}

	@XmlAttribute(required = true)
	private String description;

	@XmlAttribute(required = true)
	private String schedule;

	@XmlAttribute(required = true)
	private String command;

	public void setCommand(String command) {
		this.command = command;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public String getCommand() { return command; }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

