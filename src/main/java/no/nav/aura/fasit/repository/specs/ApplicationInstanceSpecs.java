package no.nav.aura.fasit.repository.specs;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static no.nav.aura.fasit.repository.specs.SpecHelpers.findByLifecycleStatus;
import static no.nav.aura.fasit.repository.specs.SpecHelpers.optional;

public class ApplicationInstanceSpecs {

    public static Specification<ApplicationInstance> find(final String environmentName,
                                                          final EnvironmentClass environmentClass,
                                                          final String applicationName,
                                                          final LifeCycleStatus lifeCycleStatus ) {
        final SpecHelpers<ApplicationInstance> spec = new SpecHelpers();

        optional(environmentName).ifPresent(name -> spec.and(findByEnvironment(name)));
        optional(applicationName).ifPresent(app -> spec.and(findByLikeApplicationName(app)));
        optional(environmentClass).ifPresent(envClass -> spec.and(findByEnvironmentClass(envClass)));
        optional(lifeCycleStatus).ifPresent(status -> spec.and(findByLifecycleStatus(status)));

        return spec.getSpecs();
    }

    public static Specification<ApplicationInstance> findByLikeApplicationName(final String applicationName) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("application").get("name")), "%" + applicationName.toLowerCase() + "%");
    }


    public static Specification<ApplicationInstance> findByApplication(final Application application) {
        return findByApplicationName(application.getName());
    }

    public static Specification<ApplicationInstance> findByEnvironmentAndApplication(final Environment environment, Application application) {
        return Specification
                .where(findByEnvironment(environment))
                .and(findByApplication(application));
    }

    public static Specification<ApplicationInstance> findByApplicationName(final String applicationName) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("application").get("name")), applicationName.toLowerCase());
    }

    // @Query("select ai from ApplicationInstance ai, Environment env where lower(ai.application.name) = lower(:application) and
    // ai.cluster MEMBER OF env.clusters and lower(env.name) = lower(:environment)")
    // @Query("select ai from ApplicationInstance ai, Environment env where ai.cluster MEMBER OF env.clusters and
    // lower(env.name) = lower(:environment)")
    public static Specification<ApplicationInstance> findByEnvironment(final String environment) {
        return (root, query, cb) -> {

            Subquery<Cluster> subquery = query.subquery(Cluster.class);
            Root<Environment> environments = subquery.from(Environment.class);
            Join<Environment, Cluster> envJoin = environments.join("clusters");
            subquery.select(envJoin).where(cb.equal(cb.lower(environments.get("name")), environment.toLowerCase()));

            Predicate exitsQuery = cb.in(root.get("cluster")).value(subquery);
            return exitsQuery;
        };
    }

    public static Specification<ApplicationInstance> findByEnvironment(Environment environment) {
        return findByEnvironment(environment.getName());
    }

    public static Specification<ApplicationInstance> findByEnvironmentClass(EnvironmentClass environmentClass) {
        return (root, query, cb) -> {

            Subquery<Cluster> subquery = query.subquery(Cluster.class);
            Root<Environment> environments = subquery.from(Environment.class);
            Join<Environment, Cluster> envJoin = environments.join("clusters");
            subquery.select(envJoin).where(cb.equal(environments.get("envClass"), environmentClass));

            Predicate exitsQuery = cb.in(root.get("cluster")).value(subquery);
            return exitsQuery;
        };
    }
}
