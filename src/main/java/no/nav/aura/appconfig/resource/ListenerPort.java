package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlEnum;

@XmlAccessorType(XmlAccessType.FIELD)
public class ListenerPort {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute(required = false)
    private InitialStates initialState;

    @XmlAttribute(required = false)
    private Integer maxMessages;

    @XmlAttribute(required = false)
    private Integer maxRetries;

    @XmlAttribute(required = false)
    private Integer maxSessions;

	@XmlAttribute(required = false)
	private String description;

	@XmlAttribute(required = false)
	private Boolean startOnlyOnOneNode;

    public ListenerPort(){}

    public ListenerPort(String name, InitialStates initialState, int maxMessages, int maxRetries, int maxSessions, String description, Boolean startOnlyOnOneNode) {
        this.name = name;
        this.initialState = initialState;
        this.maxMessages = maxMessages;
        this.maxRetries = maxRetries;
        this.maxSessions = maxSessions;
        this.description = description;
		this.startOnlyOnOneNode = startOnlyOnOneNode;
    }

    public String getName() {
        return name;
    }

    public InitialStates getInitialState() {
        return initialState;
    }

    public Integer getMaxMessages() {
        return maxMessages;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public Integer getMaxSessions() {
        return maxSessions;
    }

    public String getDescription() {
        return description;
    }

	public Boolean getStartOnlyOnOneNode() {
		return startOnlyOnOneNode;
	}

	@XmlEnum
    public enum InitialStates {
        START, STOP
    }
}
