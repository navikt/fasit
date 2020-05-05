package no.nav.aura.envconfig.client;

import static no.nav.aura.envconfig.client.DomainDO.EnvClass.*;
import static no.nav.aura.envconfig.client.DomainDO.Zone.FSS;
import static no.nav.aura.envconfig.client.DomainDO.Zone.SBS;

import java.util.Arrays;
import java.util.List;

public enum DomainDO {
    Devillo("devillo.no", u, FSS, SBS),
    DevilloT("devillo-t.local", u, FSS),
    TestLocal("test.local", t, FSS),
    OeraT("oera-t.local", t, SBS),
    PreProd("preprod.local", q, FSS),
    OeraQ("oera-q.local", q, SBS),
    Adeo("adeo.no", p, FSS),
    Oera("oera.no", p, SBS),
    Utvikling("utvikling.local", u, FSS);

    public enum Zone {
        FSS, SBS
    }

    public enum EnvClass {
        u, t, q, p
    }

    private final String fullyQualifiedDomainName;
    private final EnvClass envClass;
    private final List<Zone> zones;

    private DomainDO(String fqdn, EnvClass envClass, Zone... zones) {
        this.fullyQualifiedDomainName = fqdn;
        this.envClass = envClass;
        this.zones = Arrays.asList(zones);
    }

    public String getFqn() {
        return fullyQualifiedDomainName;
    }

    public EnvClass getEnvironmentClass() {
        return envClass;
    }

    public boolean isInZone(Zone zone) {
        return this.zones.contains(zone);
    }

    public static DomainDO fromFqdn(String name) {
        if (name == null) {
            return null;
        }
        for (DomainDO d : values()) {
            if (d.getFqn().equalsIgnoreCase(name)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Domain with domain name  not found: " + name);
    }

}
