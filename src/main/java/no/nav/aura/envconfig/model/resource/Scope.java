package no.nav.aura.envconfig.model.resource;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

import no.nav.aura.envconfig.model.Scopeable;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.collect.Lists;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.aura.envconfig.model.infrastructure.Zone.FSS;
import static no.nav.aura.envconfig.model.infrastructure.Zone.SBS;

/**
 * Scoping av ressurser og andre objekter i forhold til milj�, navn domene og appliaksjoner. Denne brukes b�de ved lagring, s�k
 * og til sammenligning med vekting
 */
@SuppressWarnings("serial")
@Embeddable
public class Scope implements Serializable {

    @Enumerated(EnumType.STRING)
    private EnvironmentClass envClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "env_domain")
    private Domain domain;
    private String environmentName;
    @ManyToOne
    private Application application;

    public Scope() {
    }

    public Scope(EnvironmentClass environmentClass) {
        envClass(environmentClass);
    }

    public Scope(Scope other) {
        this(other.getEnvClass());
        domain(other.getDomain());
        envName(other.getEnvironmentName());
        application(other.getApplication());
    }

    public Scope(Environment environment) {
        this(environment.getEnvClass());
        envName(environment.getName());
    }

    public final Scope envClass(EnvironmentClass envClass) {
        this.envClass = envClass;
        return this;
    }

    public final Scope domain(Domain domain) {
        this.domain = domain;
        return this;
    }

    public final Scope envName(String environmentName) {
        this.environmentName = environmentName == null ? null : environmentName.toLowerCase();
        return this;
    }

    public final Scope environment(Environment environment) {
        return  envName(environment.getName());
    }

    public final Scope application(Application application) {
        this.application = application;
        return this;
    }

    public final Scope appName(String applicationName) {
        this.environmentName = environmentName == null ? null : environmentName.toLowerCase();
        return this;
    }

    public EnvironmentClass getEnvClass() {
        return envClass;
    }

    public Domain getDomain() {
        return domain;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public Application getApplication() {
        return application;
    }

    @Override
    public String toString() {
        return "scope: " + asDisplayString();
    }

    public String getDisplayString() {
        return asDisplayString();
    }

    public String asDisplayString() {
        return (hasEnvironmentClass() ? envClass.toString() : "-") + "/" +
                (hasDomain() ? domain.getFqn() : "-") + "/" +
                (hasEnvironment() ? environmentName : "-") + "/" +
                (hasApplication() ? application : "-");
    }
    public String asDisplayStringWithZone() {
        return (hasEnvironmentClass() ? envClass.toString() : "-") + "/" +
                (hasDomain() ? (domain.isInZone(FSS) ? FSS : SBS) : "-") + "/" +
                (hasEnvironment() ? environmentName : "-") + "/" +
                (hasApplication() ? application : "-");
    }

    boolean hasDomain() {
        return domain != null;
    }

    boolean hasEnvironmentClass() {
        return envClass != null;
    }

    boolean hasEnvironment() {
        return environmentName != null;
    }

    boolean hasApplication() {
        return application != null;
    }

    /**
     * Calulating the weight of the scope giving a higher number to a more specific scope.
     */
    public int calculateScopeWeight() {
        int rating = 0;
        rating = (hasApplication()) ? rating + 8 : rating;
        rating = (hasEnvironment()) ? rating + 4 : rating;
        rating = (hasDomain()) ? rating + 2 : rating;
        rating = (hasEnvironmentClass()) ? rating + 1 : rating;
        return rating;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(envClass).append(domain).append(environmentName).append(application).build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Scope)) {
            return false;
        }
        Scope other = (Scope) obj;
        return new EqualsBuilder().append(envClass, other.envClass).append(domain, other.domain).append(environmentName, other.environmentName).append(application, other.application).build();
    }

    /**
     * Return the element from the collection that has the highest scope weight. All elements in input must have a scope that
     * matches this scope
     *
     * @param collection of Scopable objects
     * @return the best match related to scopeWeight
     */
    public <T extends Scopeable> T singleBestMatch(Collection<T> collection) {

        verifyScopeCorrectness(collection);

        List<T> elements = Lists.newArrayList(collection);
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("Cant find best match for a empty collection");
        }

        if (elements.size() == 1) {
            return elements.iterator().next();
        }

        Collections.sort(elements, new ScopeWeightComparator<T>());
        Collections.reverse(elements);

        T bestMatch = elements.get(0);
        T nextBestMatch = elements.get(1);
        if (bestMatch.getScope().calculateScopeWeight() == nextBestMatch.getScope().calculateScopeWeight()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageFormat.format("Input contains at least two resources of type {0} with same scope {1} \n Input: {2}", bestMatch.getClass(), bestMatch.getScope(), collection));
        } else {
            return bestMatch;
        }
    }

    private <T extends Scopeable> void verifyScopeCorrectness(Collection<T> collection) {
        for (T element : collection) {
            Scope inputScope = element.getScope();
            if (isSetAndNotMatching(envClass, inputScope.envClass)) {
                throw new IllegalArgumentException("Input scope contains illegal environmentClass " + inputScope + " expected " + this);
            }
            if (isSetAndNotMatching(domain, inputScope.domain)) {
                throw new IllegalArgumentException("Input scope contains illegal domain " + inputScope + " expected " + this);
            }
            if (isSetAndNotMatching(environmentName, inputScope.environmentName)) {
                throw new IllegalArgumentException("Input scope contains illegal environmentName " + inputScope + " expected " + this);
            }
            if (isSetAndNotMatching(application, inputScope.application)) {
                throw new IllegalArgumentException("Input scope contains illegal application " + inputScope + " expected " + this);
            }
        }
    }

    private static <T> boolean isSetAndNotMatching(T search, T input) {
        return search != null && input != null && !search.equals(input);
    }

    private static <T> boolean isSubset(T a, T b) {
        if (b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }

        return a.equals(b);
    }

    /**
     * Checking if another scope is a subset of this scope. Every parameter is checked for equal or if the superscope is null
     * (*)
     */
    public boolean isSubsetOf(Scope otherScope) {
        if (!isSubset(envClass, otherScope.getEnvClass())) {
            return false;
        }
        if (!isSubset(domain, otherScope.domain)) {
            return false;
        }
        if (!isSubset(environmentName, otherScope.environmentName)) {
            return false;
        }
        if (!isSubset(application, otherScope.application)) {
            return false;
        }

        return true;
    }

}
