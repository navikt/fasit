package no.nav.aura.fasit.repository.specs;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static no.nav.aura.fasit.repository.specs.SpecHelpers.findByLifecycleStatus;
import static no.nav.aura.fasit.repository.specs.SpecHelpers.optional;

public class ResourceSpecs {

    private Specification<Resource> specs = null;

    public static Specification<Resource> findByLikeAlias(
            final String alias,
            final ResourceType type,
            final EnvironmentClass environmentClass,
            final Optional<Environment> environment,
            final Optional<Domain> domain,
            final Optional<Application> application,
            final LifeCycleStatus lifecycleStatus) {

        SpecHelpers<Resource> resourceSpecs = new SpecHelpers();

        optional(alias).ifPresent(a -> resourceSpecs.and(findByLikeAlias(a)));
        return find(resourceSpecs, type, environmentClass, environment, domain, application, lifecycleStatus).getSpecs();
    }

    public static Specification<Resource> findByExcactAlias(
            final String alias,
            final ResourceType type,
            final EnvironmentClass environmentClass,
            final Optional<Environment> environment,
            final Optional<Domain> domain,
            final Optional<Application> application) {


        SpecHelpers<Resource> resourceSpecs = new SpecHelpers();

        optional(alias).ifPresent(a -> resourceSpecs.and(findByAlias(a)));
        return find(resourceSpecs, type, environmentClass, environment, domain, application, null).getSpecs();
    }

    private static SpecHelpers<Resource> find(SpecHelpers<Resource> resourceSpecs,
                                      final ResourceType type,
                                      final EnvironmentClass environmentClass,
                                      final Optional<Environment> environment,
                                      final Optional<Domain> domain,
                                      final Optional<Application> application,
                                      final LifeCycleStatus lifeCycleStatus) {
        optional(type).ifPresent(t -> resourceSpecs.and(findByType(type)));
        optional(environmentClass).ifPresent(ec -> resourceSpecs.and(findByEnvironmentClass(ec)));

        environment.ifPresent(e -> resourceSpecs.and(findByEnvironment(e)));
        domain.ifPresent(d -> resourceSpecs.and(findByDomain(d)));
        application.ifPresent(a -> resourceSpecs.and(findByApplication(a)));
        optional(lifeCycleStatus).ifPresent(l -> resourceSpecs.and(findByLifecycleStatus(l)));

        return resourceSpecs;
    }


    public static Specification<Resource> findByScope(
            String alias,
            ResourceType type,
            EnvironmentClass environmentClass,
            Optional<Environment> environment,
            Optional<Domain> domain,
            Optional<Application> application) {

        SpecHelpers<Resource> resourceSpecs = new SpecHelpers<>();
        SpecHelpers<Resource> scopeSpec = new SpecHelpers<>();

        optional(alias).ifPresent(a -> resourceSpecs.and(findByAlias(a)));
        optional(type).ifPresent(t -> resourceSpecs.and(findByType(type)));

        optional(environmentClass).ifPresent(ec -> scopeSpec.or(findByEnvironmentClass(ec)));
        environment.ifPresent(e -> scopeSpec.or(findByEnvironment(e)));
        domain.ifPresent(d -> scopeSpec.or(findByDomain(d)));
        application.ifPresent(a -> scopeSpec.or(findByApplication(a)));

        return resourceSpecs.getSpecs().and(scopeSpec.getSpecs());
    }

    public static Specification<Resource> findByScope(
            String alias,
            ResourceType type,
            EnvironmentClass environmentClass) {
        return findByScope(alias, type, environmentClass, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Specification<Resource> findByAlias(final String alias) {
        return (root, criteriaQuery, cb) -> cb.equal(cb.lower(root.get("alias")), alias.toLowerCase());
    }

    public static Specification<Resource> findByLikeAlias(final String alias) {
        return (root, criteriaQuery, cb) -> cb.like(cb.lower(root.get("alias")), "%" + alias.toLowerCase() + "%");
    }

    public static Specification<Resource> findByType(final ResourceType type) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get("type"), type);
    }

        public static Specification<Resource> findByEnvironmentClass(final EnvironmentClass envClass) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get("scope").get("envClass"), envClass);
    }

    public static Specification<Resource> findByEnvironment(final Environment environment) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get("scope").get("environmentName"), environment.getName());
    }

    public static Specification<Resource> findByDomain(final Domain domain) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get("scope").get("domain"), domain);

    }

    public static Specification<Resource> findByApplication(final Application app) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get("scope").get("application"), app);
    }
}
