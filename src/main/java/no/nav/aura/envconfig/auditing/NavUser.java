package no.nav.aura.envconfig.auditing;

import java.io.Serializable;

public class NavUser implements Serializable {

    private static final long serialVersionUID = 5319570271673284399L;

    private String id;
    private String name;

    private boolean exists;

    public NavUser(String id) {
        this.id = id;
    }

    public NavUser(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setName(String firstname, String lastname) {
        this.name = firstname + " " + lastname;
    }

    public String getDisplayName() {
        if (name != null) {
            return String.format("%s (%s)", name, id);
        }
        return id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public boolean exists() {
        return this.exists;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public boolean isServiceUser() {
        return id.toLowerCase().startsWith("srv");
    }
}
