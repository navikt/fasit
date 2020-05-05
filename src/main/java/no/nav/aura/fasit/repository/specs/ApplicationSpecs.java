package no.nav.aura.fasit.repository.specs;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import org.springframework.data.jpa.domain.Specification;

import static no.nav.aura.fasit.repository.specs.SpecHelpers.findByLifecycleStatus;
import static no.nav.aura.fasit.repository.specs.SpecHelpers.optional;

public class ApplicationSpecs {

    public static Specification<Application> find(
            final String name, LifeCycleStatus lifeCycleStatus) {

        final SpecHelpers<Application> spec = new SpecHelpers();

        optional(name).ifPresent(n -> spec.and(findByLikeName(n)));
        optional(lifeCycleStatus).ifPresent(status -> spec.and(findByLifecycleStatus(lifeCycleStatus)));

        return spec.getSpecs();
    }

    private static Specification<Application> findByLikeName(final String name) {
        return (root, criteriaQuery, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }



}
