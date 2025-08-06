package no.nav.aura.appconfig.artifact;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClassLoader extends Artifact {

    @XmlEnum
    public enum Type {
        APPLICATION, MODULE, SERVER
    };

    @XmlEnum
    public enum Mode {
        PARENT_FIRST, PARENT_LAST
    };

    @XmlEnum
    public enum Policy {
        SINGLE, MULTIPLE
    };

    @XmlAttribute(name = "type", required = false)
    private Type type = Type.APPLICATION;

    @XmlAttribute(name = "mode", required = false)
    private Mode mode = Mode.PARENT_FIRST;

    @XmlAttribute(name = "policy", required = false)
    private Policy policy = Policy.MULTIPLE;

    public ClassLoader() {
    }

    public ClassLoader(Type type, Mode mode, Policy policy) {
        this.type = type;
        this.mode = mode;
        this.policy = policy;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }
}
