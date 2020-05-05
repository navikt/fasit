package no.nav.aura.fasit.repository.specs;

import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import org.springframework.data.jpa.domain.Specification;

import static no.nav.aura.fasit.repository.specs.SpecHelpers.findByLifecycleStatus;
import static no.nav.aura.fasit.repository.specs.SpecHelpers.optional;

public class EnvironmentSpecs {

    public static Specification<Environment> find(
            final EnvironmentClass environmentClass,
            final String name,
            final LifeCycleStatus lifeCycleStatus) {

        final SpecHelpers<Environment> spec = new SpecHelpers();

        optional(environmentClass).ifPresent(envClass -> spec.and(findByEnvironmentClass(environmentClass)));
        optional(name).ifPresent(n -> spec.and(findByLikeName(n)));
        optional(lifeCycleStatus).ifPresent(l -> spec.and(findByLifecycleStatus(l)));

        return spec.getSpecs();
    }


    private static Specification<Environment> findByEnvironmentClass(final EnvironmentClass environmentClass) {
        return (root, criteriaQuery, cb) -> cb.equal(root.get("envClass"), environmentClass);
    }

    private static Specification<Environment> findByLikeName(final String name) {
        return (root, criteriaQuery, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }
}
