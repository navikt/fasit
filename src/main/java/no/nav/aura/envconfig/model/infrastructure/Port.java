package no.nav.aura.envconfig.model.infrastructure;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.io.Serializable;

@SuppressWarnings("serial")
@Entity
@Table(name = "port")
@Audited
public class Port implements Serializable {

    @Id
    @SequenceGenerator(name = "hibernate_sequence", sequenceName = "hibernate_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @Column(name = "port_entid")
    public Long id;

    public String hostname;

    @Column(name = "portnumber")
    public int number;

    public String type;

    public Port(final String hostname, final int number, final String type) {
        this.hostname = hostname;
        this.number = number;
        this.type = type;
    }

    public Port() {
    }

    @Override
    public String toString() {
        return hostname + ":" + number + "/" + type;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(toString()).build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Port)) {
            return false;
        }

        return obj.toString().equalsIgnoreCase(toString());
    }
}
