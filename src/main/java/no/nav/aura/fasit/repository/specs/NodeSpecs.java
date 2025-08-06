package no.nav.aura.fasit.repository.specs;

import no.nav.aura.envconfig.model.deletion.LifeCycleStatus;
import no.nav.aura.envconfig.model.infrastructure.*;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.util.Collection;

import static no.nav.aura.fasit.repository.specs.SpecHelpers.findByLifecycleStatus;
import static no.nav.aura.fasit.repository.specs.SpecHelpers.optional;

public class NodeSpecs {

    public static Specification<Node> find(final String environmentName,
                                           final EnvironmentClass environmentClass,
                                           final PlatformType type,
                                           final String hostname,
                                           final String application,
                                           final LifeCycleStatus lifeCycleStatus) {

        SpecHelpers<Node> nodeSpec = new SpecHelpers<>();

        optional(environmentClass).ifPresent(envClass -> nodeSpec.and(findByEnvironmentClass(envClass)));
        optional(environmentName).ifPresent(env -> nodeSpec.and(findByEnvironment(env)));
        optional(hostname).ifPresent(host -> nodeSpec.and(findByLikeHostname(host)));
        optional(type).ifPresent(t -> nodeSpec.and(findByType(t)));
        optional(application).ifPresent(app -> nodeSpec.and(findByApplication(app)));
        optional(lifeCycleStatus).ifPresent(status -> nodeSpec.and(findByLifecycleStatus(status)));

        return nodeSpec.getSpecs();
    }

    public static Specification<Node> findByEnvironmentClass(final EnvironmentClass environmentClass) {
        return (root, query, cb) -> {

            Subquery<Node> subQuery = query.subquery(Node.class);
            Root<Environment> subQueryFrom = subQuery.from(Environment.class);
            Join<Environment, Node> nodesJoin = subQueryFrom.join("nodes");
            subQuery.select(nodesJoin).where(cb.equal(subQueryFrom.get("envClass"), environmentClass));

            Predicate exitsQuery = cb.in(root).value(subQuery);
            return exitsQuery;

        };
    }

    public static Specification<Node> findByEnvironment(final String environment) {
        return (root, query, cb) -> {

            Subquery<Node> subquery = query.subquery(Node.class);
            Root<Environment> environments = subquery.from(Environment.class);
            Join<Environment, Node> envJoin = environments.join("nodes");
            subquery.select(envJoin).where(cb.equal(cb.lower(environments.get("name")), environment.toLowerCase()));

            Predicate exitsQuery = cb.in(root).value(subquery);
            return exitsQuery;

        };
    }
    
    
    public static Specification<Node> findByApplication(final String application) {
        return (root, query, cb) -> {

            Subquery<ApplicationInstance> subquery = query.subquery(ApplicationInstance.class);
            Root<ApplicationInstance> appsRoot = subquery.from(ApplicationInstance.class);
            Predicate appNameEquals = cb.equal(cb.lower(appsRoot.get("application").get("name")), application.toLowerCase());
            Expression<Collection<Cluster>> collection = root.get("clusters");
            Path<Cluster> appClusterPath = appsRoot.get("cluster");
            Expression<Boolean> clusterMemberOf = cb.isMember( appClusterPath,  collection);
            subquery.select(appsRoot).where(cb.and(appNameEquals, clusterMemberOf));
            Predicate exitsQuery = cb.exists(subquery);
            return exitsQuery;
        };
    }

    public static Specification<Node> findByLikeHostname(final String hostname) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("hostname")), "%" + hostname.toLowerCase() + "%");

    }

    public static Specification<Node> findByType(final PlatformType type) {
        return (root, query, cb) -> cb.equal(root.get("platformType"), type);

    }
}
