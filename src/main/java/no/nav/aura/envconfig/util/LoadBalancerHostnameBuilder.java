package no.nav.aura.envconfig.util;

import static java.lang.String.format;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Zone;

public class LoadBalancerHostnameBuilder {

    public static String create(Domain domain, String environmentName) {
        return format("%s.%s", getSubdomain(domain, environmentName), getLbDomain(domain));
    }

    private static String getLbDomain(Domain domain) {
        if (domain.isInZone(Zone.FSS)) {
            return "adeo.no";
        }
        if (domain.isInZone(Zone.SBS)) {
            return "oera.no";
        }
        throw new RuntimeException("Unknown zone, " + domain);
    }

    private static String getSubdomain(Domain domain, String environmentName) {
        String postfix = "p".equalsIgnoreCase(environmentName) ? "" : "-" + environmentName;
        if (domain.isInZone(Zone.FSS)) {
            return "app" + postfix;
        }
        if (domain.isInZone(Zone.SBS)) {
            return "itjenester" + postfix;
        }
        throw new RuntimeException("Unknown zone, " + domain);
    }
}
