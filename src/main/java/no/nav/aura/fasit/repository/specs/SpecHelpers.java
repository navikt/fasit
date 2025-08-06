package no.nav.aura.fasit.repository.specs;

import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public class  SpecHelpers<T> {

    private Specification<T> specs = null;


    public   void and(Specification<T> spec) {
        if (specs == null) {
            specs = Specification.where(spec);
        }
        specs = specs.and(spec);
    }

    public void or(Specification<T> spec) {
        if (specs == null) {
            specs = Specification.where(spec);
        }
        specs = specs.or(spec);

    }

    public static <P> Optional<P> optional(P property) {
        return Optional.ofNullable(property);
    }

    public  static <T> Specification<T> findByLifecycleStatus(final LifeCycleStatus lifeCycleStatus) {
        return ((root, criteriaQuery, cb) -> cb.equal(root.get("lifeCycleStatus"), lifeCycleStatus));
    }

    public Specification<T> getSpecs() {
        return specs;
    }

}
