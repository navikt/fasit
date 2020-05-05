package no.nav.aura.appconfig.deprecated;

import no.nav.aura.appconfig.Application;
import no.nav.aura.appconfig.LoadBalancer;

import java.util.*;

public class Deprecations {

    private Set<Deprecation> deprecations = new HashSet<>();

    public Deprecations(Application application) {
        LoadBalancer loadBalancer = application.getLoadBalancer();
        if (loadBalancer != null) {
            if (loadBalancer.getContextRoots() != null && !loadBalancer.getContextRoots().isEmpty()) {
                add("contextRoot", Deprecation.date(2017, Calendar.SEPTEMBER, 30), "contextRoot is removed from loadBalancer");
            }
        }
    }

    private void add(String tag, Date from, String suggestion) {
        deprecations.add(new Deprecation(tag, from, suggestion));
    }

    public Collection<String> getWarnings() {
        Set<String> warnings = new HashSet<>();
        for (Deprecation dep : deprecations) {
            if (!dep.isExpired()) {
                warnings.add(String.format("Element %s is deprecated and will not be supported after %tF. %s", dep.getTag(), dep.getFrom(), dep.getSuggestion()));
            }
        }
        return warnings;

    }

    public Collection<String> getErrors() {
        Set<String> errors = new HashSet<>();
        for (Deprecation dep : deprecations) {
            if (dep.isExpired()) {
                errors.add(String.format("Element %s has been deprecated since %tF. %s", dep.getTag(), dep.getFrom(), dep.getSuggestion()));
            }
        }
        return errors;

    }

    public Set<Deprecation> get() {
        return deprecations;
    }

    protected void set(Set<Deprecation> deprecations) {
        this.deprecations = deprecations;
    }

}
