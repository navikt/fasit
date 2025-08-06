package no.nav.aura.envconfig;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.AdditionalRevisionInfo;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.util.SerializableFunction;
import no.nav.aura.envconfig.util.Tuple;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class JPAFasitRepository implements FasitRepository {

    private static Logger log = LoggerFactory.getLogger(JPAFasitRepository.class);

    @PersistenceContext
    private EntityManager em;


    @Autowired
    private DataSource ds;

    private String connectionString;

    public JPAFasitRepository() {
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public <T extends ModelEntity> T store(T entity) {
        T persistedEntity = em.merge(entity);
        em.flush();
        return persistedEntity;
    }

    @Override
    public List<Tuple<Long, RevisionType>> getRevisionsFor(Class<? extends ModelEntity> entityClass, Long entityId) {
        if (entityId == null) {
            return Lists.newArrayList();
        }
        AuditReader auditReader = AuditReaderFactory.get(em);
        @SuppressWarnings("unchecked")
        List<Object[]> revisions = auditReader.createQuery().forRevisionsOfEntity(entityClass, entityClass.getName(), false, true).add(AuditEntity.id().eq(entityId)).getResultList();
        @SuppressWarnings("serial")
        List<Tuple<Long, RevisionType>> entityRevisions = FluentIterable.from(revisions).transform(new SerializableFunction<Object[], Tuple<Long, RevisionType>>() {
            public Tuple<Long, RevisionType> process(Object[] objects) {
                return Tuple.of(((AdditionalRevisionInfo<?>) objects[1]).getRevision(), ((RevisionType) objects[2]));
            }
        }).toSortedList(Tuple.fstComparator(Ordering.natural().reverse()));
        return entityRevisions;
    }

    @Override
    public <T extends ModelEntity> FasitRevision<T> getRevision(Class<T> entityClass, long entityId, long revision) {
        AuditReader auditReader = AuditReaderFactory.get(em);
        @SuppressWarnings("unchecked")
        AdditionalRevisionInfo<T> revInfo = auditReader.findRevision(AdditionalRevisionInfo.class, revision);
        T historicEntity = auditReader.find(entityClass, entityId, revision);
        return new FasitRevision<T>(revInfo, historicEntity);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<AdditionalRevisionInfo<ModelEntity>> findHistory(Class<?> entityType, Date from, String authorId, long startIdx, int count, String orderBy, boolean sortAscending) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<AdditionalRevisionInfo> query = em.getCriteriaBuilder().createQuery(AdditionalRevisionInfo.class);
        Root<AdditionalRevisionInfo> root = query.from(AdditionalRevisionInfo.class);

        List<Predicate> predicates = createHistoryPredicates(entityType, from, authorId, builder, root);

        query.where(predicates.toArray(new Predicate[] {}));
        Path<Object> orderByPath = root.get(orderBy);
        query.orderBy(sortAscending ? builder.asc(orderByPath) : builder.desc(orderByPath));
        // Cheating; generics in generics are not pretty in java
        List resultList = em.createQuery(query).setMaxResults(count).setFirstResult((int) startIdx).getResultList();
        return (List<AdditionalRevisionInfo<ModelEntity>>) resultList;
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public long countHistory(Class<?> entityType, Date from, String authorId) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<AdditionalRevisionInfo> root = query.from(AdditionalRevisionInfo.class);
        List<Predicate> predicates = createHistoryPredicates(entityType, from, authorId, builder, root);

        query.select(builder.count(root));
        query.where(predicates.toArray(new Predicate[] {}));
        return (Long) em.createQuery(query).getSingleResult();
    }

    @SuppressWarnings("rawtypes")
    private List<Predicate> createHistoryPredicates(Class<?> entityType, Date from, String authorId, CriteriaBuilder builder, Root<AdditionalRevisionInfo> root) {
        List<Predicate> predicates = new ArrayList<Predicate>();
        if (entityType != null) {
            predicates.add(builder.equal(root.get("modifiedEntityType"), entityType));
        }
        if (authorId != null) {
            predicates.add(builder.equal(root.get("authorId"), authorId));
        }
        if (from != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.<Date> get("timestamp"), from));
        }
        return predicates;
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Override
    public <T> List<Tuple<T, RevisionType>> getEntitiesForRevision(Class<T> entityClass, long revision) {
        AuditReader auditReader = AuditReaderFactory.get(em);
        List<Object[]> resultList = auditReader.createQuery().forRevisionsOfEntity(entityClass, entityClass.getName(), false, true).add(AuditEntity.revisionNumber().eq(revision)).getResultList();
        return FluentIterable.from(resultList).transform(new SerializableFunction<Object[], Tuple<T, RevisionType>>() {
            public Tuple<T, RevisionType> process(Object[] input) {
                return Tuple.of((T) input[0], (RevisionType) input[2]);
            }
        }).toList();
    }

    @Override
    public Environment findEnvironmentBy(String name) {
        return getSingleResultOrNull(em.createQuery("select e from Environment e where lower(e.name)=:envName", Environment.class).setParameter("envName", name.toLowerCase()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(ExposedServiceReference exposed) {
        log.debug("Deleting exposed resource ", exposed.getID());
        Resource resource = exposed.getResource();
        // Do not remove all kind of resources
        if (ResourceType.externalExposedResourceTypes.contains(resource.getType())) {
            // hack for å ikke slette queue osv med cascading
            exposed.setResource(null);
            log.info("Will not delete resource {} even though application exposing it is deleted");
        }
        remove(exposed);

    }

    private void remove(ModelEntity entity) {
        if (!em.contains(entity)) {
            entity = em.merge(entity);
        }
        em.remove(entity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(ModelEntity entity) {
        // removing nodes from parent
        if (entity instanceof Node) {
            Node node = (Node) entity;
            Environment environment = getEnvironmentBy(node);
            if (environment != null) {
                log.debug("Removing node {} from environment {} ", node.getHostname(), environment.getName());
                environment.removeNode(node);
                store(environment);
            }
        } else if (entity instanceof Cluster) {
            Cluster cluster = (Cluster) entity;
            Environment environment = getEnvironmentBy(cluster);
            environment.removeCluster(cluster);
            store(environment);
        } else if (entity instanceof Application) {
            Application application = (Application) entity;
            List<ApplicationInstance> applicationInstances = findApplicationInstancesBy(application);
            for (ApplicationInstance applicationInstance : applicationInstances) {
                remove(applicationInstance);
            }
        } else if (entity instanceof Resource) {
            final Resource resource = (Resource) entity;
            ApplicationInstance applicationInstance = findApplicationInstanceByExposedResourceId(resource.getID());
            if (applicationInstance != null) {
                log.debug("Removing exposed resource {} from application instance {} ", resource, applicationInstance);
                Set<ExposedServiceReference> exposedServices = applicationInstance.getExposedServices();
                Optional<ExposedServiceReference> findExposed = FluentIterable.from(exposedServices).firstMatch(new com.google.common.base.Predicate<ExposedServiceReference>() {

                    @Override
                    public boolean apply(ExposedServiceReference input) {
                        return input.getResource().getID().equals(resource.getID());
                    }
                });
                if (findExposed.isPresent()) {
                    applicationInstance.getExposedServices().remove(findExposed.get());
                    remove(findExposed.get());
                    return;
                }
            }
        }

        remove(entity);
    }

    @Override
    public List<Environment> findEnvironmentsBy(EnvironmentClass environmentClass) {
        return em.createQuery("select e from Environment e where e.envClass=?1", Environment.class).setParameter(1, environmentClass).getResultList();
    }

    @Override
    public List<Resource> findResourcesByExactAlias(Scope scope, ResourceType type, String alias) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Resource> query = builder.createQuery(Resource.class);
        Root<Resource> resource = query.from(Resource.class);
        query.where(resourceQueryWithEqualAlias(scope, type, alias, builder, resource));

        TypedQuery<Resource> resourceQuery = em.createQuery(query);
        return new ArrayList<Resource>(resourceQuery.getResultList());
    }

    @Override
    public List<Resource> findResourcesByLikeAlias(Scope scope, ResourceType type, String alias, int first, int count, String sortByProperty, boolean sortAscending) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Resource> query = builder.createQuery(Resource.class);

        Root<Resource> resource = query.from(Resource.class);
        query.where(resourceQueryWithLikeAlias(scope, type, alias, builder, resource));
        if (sortByProperty != null) {
            Path<?> sortByPath = resource;
            String[] elements = sortByProperty.split("\\.");
            for (String element : elements) {
                sortByPath = sortByPath.get(element);
            }

            query.orderBy(sortAscending ? builder.asc(sortByPath) : builder.desc(sortByPath));
        }

        TypedQuery<Resource> typedQuery = em.createQuery(query);
        typedQuery.setFirstResult(first);
        typedQuery.setMaxResults(count);

        return new ArrayList<>(typedQuery.getResultList());
    }

    public long findNrOfResources(Scope scope, ResourceType type, String alias) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<Resource> resource = query.from(Resource.class);

        query.select(builder.count(resource));
        query.where(resourceQueryWithLikeAlias(scope, type, alias, builder, resource));

        return em.createQuery(query).getSingleResult();
    }

    private Predicate resourceQueryWithEqualAlias(Scope scope, ResourceType type, String alias, CriteriaBuilder builder, Root<Resource> resource) {
        Predicate predicate = resourceQuery(scope, type, builder, resource);
        if (alias != null) {
            predicate = builder.and(predicate, builder.equal(builder.lower(resource.<String> get("alias")), alias.toLowerCase()));
        }
        return predicate;
    }

    private Predicate resourceQueryWithLikeAlias(Scope scope, ResourceType type, String alias, CriteriaBuilder builder, Root<Resource> resource) {
        Predicate predicate = resourceQuery(scope, type, builder, resource);
        if (alias != null) {
            predicate = builder.and(predicate, builder.like(builder.lower(resource.<String> get("alias")), "%" + alias.toLowerCase() + "%"));
        }
        return predicate;
    }

    private Predicate resourceQuery(Scope scope, ResourceType type, CriteriaBuilder builder, Root<Resource> resource) {
        Path<Scope> scopePath = resource.get("scope");
        Predicate predicate = createScopePredicates(scope, builder, scopePath);
        if (type != null) {
            predicate = builder.and(predicate, builder.equal(resource.get("type"), type));
        }
        return predicate;
    }

    /**
     * Hibernate predicate for filtering out scope. NB Not including application scope use filterApplicationScope
     */
    private Predicate createScopePredicates(Scope scope, CriteriaBuilder builder, Path<Scope> scopePath) {
        List<Predicate> predicates = Lists.newArrayList();
        if (scope.getEnvClass() != null) {
            predicates.add(builder.equal(scopePath.get("envClass"), scope.getEnvClass()));
        }
        if (scope.getEnvironmentName() != null) {
            Path<String> environmentNamePath = scopePath.get("environmentName");
            predicates.add(builder.or(builder.equal(environmentNamePath, scope.getEnvironmentName()), builder.isNull(environmentNamePath)));
        }
        if (scope.getDomain() != null) {
            Path<Domain> domainPath = scopePath.get("domain");
            predicates.add(builder.or(builder.equal(domainPath, scope.getDomain()), builder.isNull(domainPath)));
        }
        if (scope.getApplication() != null) {
            Path<Object> applicationPath = scopePath.get("application");
            predicates.add(builder.or(builder.equal(applicationPath, scope.getApplication()), builder.isNull(applicationPath)));
        }
        return builder.and(predicates.toArray(new Predicate[predicates.size()]));
    }

    @Override
    public String getConnectionDescription() {
        if (connectionString == null) {
            try (Connection connection = ds.getConnection()) {
                connectionString = connection.getMetaData().getUserName() + "@" + connection.getMetaData().getURL();
            } catch (SQLException e) {
                log.warn("Error retrieving database user metadata", e);
            }
        }
        return connectionString;
    }

    @Override
    public Application findApplicationByName(String applicationName) {
        if (applicationName == null || applicationName.isEmpty()) {
            throw new IllegalArgumentException("Application name can not be null or empty");
        }

        return getSingleResultOrNull(em.createQuery("select a from Application a where lower(a.name)=?1", Application.class).setParameter(1, applicationName.toLowerCase()));
    }

    @Override
    public Set<Application> getApplications() {
        return new HashSet<Application>(em.createQuery("from Application", Application.class).getResultList());
    }

    @Override
    public Set<Application> getApplicationsNotInApplicationGroup() {
        TypedQuery<Application> query = em.createQuery(
                "from Application app where app not in (select a from ApplicationGroup ag, IN(ag.applications) a)",
                Application.class);
        return new HashSet<Application>(query.getResultList());
    }

    @Override
    public <T> T getById(Class<T> entityClass, long id) {
        T entity = em.find(entityClass, id);
        if (entity == null) {
            throw new NoResultException("Unable to find id " + id + " for entity type " + entityClass.getSimpleName());
        }
        return entity;
    }

    @Override
    public <T> long count(Class<T> entityClass) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        query.select(builder.count(query.from(entityClass)));
        return (Long) em.createQuery(query).getSingleResult();
    }

    @Override
    public List<Environment> getEnvironments() {
        return em.createQuery("from Environment ORDER BY name", Environment.class).getResultList();
    }

    @Override
    public Node findNodeBy(String hostName) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Node> query = builder.createQuery(Node.class);
        Root<Node> from = query.from(Node.class);
        Predicate predicate = builder.equal(builder.lower(from.<String> get("hostname")), hostName.toLowerCase());
        CriteriaQuery<Node> select = query.where(predicate);
        Node node = getSingleResultOrNull(em.createQuery(select));
        return node;
    }

    /**
     * JPA getSingleResult kaster exception ved null treff, denne returnere null
     */
    private <T> T getSingleResultOrNull(TypedQuery<T> query) {
        List<T> resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return null;
        }
        if (resultList.size() == 1) {
            return resultList.get(0);
        }
        throw new RuntimeException("query returned more than one hit. " + resultList);

    }

    @Override
    public Environment getEnvironmentBy(ApplicationInstance entity) {
        // if (entity instanceof Cluster && ((Cluster) entity).getID() != null) {
        // String q = "select ep from Environment ep inner join ep.clusters c where c.id = ?1";
        // return em.createQuery(q, Environment.class).setParameter(1, entity.getID()).getSingleResult();
        // }
        // if (entity instanceof Node && !entity.isNew()) {
        // String q = "select ep from Environment ep inner join ep.nodes n where n.id = ?1";
        // return em.createQuery(q, Environment.class).setParameter(1, entity.getID()).getSingleResult();
        // }
        if (entity instanceof ApplicationInstance) {
            return getEnvironmentBy(((ApplicationInstance) entity).getCluster());
        }
        throw new RuntimeException("Unable to handle access check of entity " + entity.getClass());
    }

    @Override
    public Environment getEnvironmentBy(Node node) {
        return getSingleResultOrNull(em.createQuery("select ep from Environment ep where :node MEMBER OF ep.nodes", Environment.class).setParameter("node", node));
    }

    @Override
    public Environment getEnvironmentBy(Cluster cluster) {
        String query = "select e from Environment e join e.clusters clusters  where clusters.id = :clusterId";
        return em.createQuery(query, Environment.class).setParameter("clusterId", cluster.getID()).getSingleResult();
    }

    @Override
    public List<ApplicationInstance> findApplicationInstancesBy(Application application) {
        if (application.isNew()) {
            return Lists.newArrayList();
        }
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<ApplicationInstance> query = builder.createQuery(ApplicationInstance.class);
        Root<ApplicationInstance> from = query.from(ApplicationInstance.class);
        return em.createQuery(query.where(builder.equal(from.get("application"), application))).getResultList();
    }

    @Override
    public Set<Cluster> findClustersBy(ApplicationGroup applicationGroup) {

        String query = "select distinct c from Cluster c join c.applications clusterApps join " +
                "clusterApps.application app where app in " +
                "(select agApps from ApplicationGroup ag join ag.applications agApps where ag.id = :applicationGroupId)";
        return Sets.newHashSet(em.createQuery(query, Cluster.class).setParameter("applicationGroupId", applicationGroup.getID()).getResultList());
    }

    @Override
    public List<Resource> findDuplicateProperties(Resource resource) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Resource> query = builder.createQuery(Resource.class);
        Root<Resource> resourceRoot = query.from(Resource.class);
        List<Predicate> clauses = Lists.newArrayList();
        for (Entry<String, String> entry : resource.getProperties().entrySet()) {
            MapJoin<Resource, String, String> properties = resourceRoot.joinMap("properties", JoinType.INNER);
            clauses.add(builder.and(builder.equal(properties.key(), entry.getKey()), builder.equal(properties.value(), entry.getValue())));
        }
        if (!resource.isNew()) {
            clauses.add(builder.notEqual(resourceRoot.get("id"), resource.getID()));
        }
        clauses.add(builder.equal(resourceRoot.get("type"), resource.getType()));
        return em.createQuery(query.where(clauses.toArray(new Predicate[clauses.size()]))).getResultList();
    }

    @Override
    public List<Resource> findOverlappingResourceScope(final Resource resource) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Resource> query = builder.createQuery(Resource.class);
        Root<Resource> resourceRoot = query.from(Resource.class);
        Path<Scope> scopePath = resourceRoot.get("scope");
        Predicate idNotEquals = builder.not(equalOrNull(builder, resourceRoot.get("id"), resource.getID()));
        Predicate envClassEqual = equalOrNull(builder, scopePath.get("envClass"), resource.getScope().getEnvClass());
        Predicate envNameEqual = equalOrNull(builder, scopePath.get("environmentName"), resource.getScope().getEnvironmentName());
        Predicate domainEqual = equalOrNull(builder, scopePath.get("domain"), resource.getScope().getDomain());
        Predicate aliasEqual = builder.equal(resourceRoot.get("alias"), resource.getAlias());
        Predicate typeEqual = builder.equal(resourceRoot.get("type"), resource.getType());
        List<Resource> resultList = em.createQuery(query.where(builder.and(idNotEquals, envClassEqual, envNameEqual, domainEqual, aliasEqual, typeEqual))).getResultList();
        // TODO Simplify this when we have reduced Scope.application to application
        return FluentIterable.from(resultList).filter(new com.google.common.base.Predicate<Resource>() {
            public boolean apply(@Nullable Resource input) {
                assert input != null;
                return new EqualsBuilder().append(resource.getScope().getApplication(), input.getScope().getApplication()).isEquals();
            }

        }).toList();
    }

    private <T> Predicate equalOrNull(CriteriaBuilder builder, Path<T> path, T object) {
        if (object == null) {
            return builder.isNull(path);
        }
        return builder.equal(path, object);
    }

    @Override
    @Nullable
    public ApplicationInstance findApplicationInstanceByExposedResourceId(Long resourceId) {
        String q = "select ai from ApplicationInstance ai join ai.exposedServices es join es.resource res where res.id = :resourceId";
        TypedQuery<ApplicationInstance> query = em.createQuery(q, ApplicationInstance.class).setParameter("resourceId", resourceId);
        return getSingleResultOrNull(query);
    }

    @Override
    public Collection<ResourceReference> findFutureResourceReferencesBy(String resourceName, ResourceType resourceType) {
        String q = "select rr from ResourceReference rr where alias = ?1 and resourceType = ?2 and future = true and applicationinstance_entid is not null";
        return em.createQuery(q, ResourceReference.class).setParameter(1, resourceName).setParameter(2, resourceType).getResultList();
    }

    @Override
    public ApplicationInstance getApplicationInstanceBy(ResourceReference resourceReference) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<ApplicationInstance> query = builder.createQuery(ApplicationInstance.class);
        Root<ApplicationInstance> root = query.from(ApplicationInstance.class);
        Path<Set<ResourceReference>> resourceReferencesPath = root.get("resourceReferences");
        return em.createQuery(query.where(builder.isMember(resourceReference, resourceReferencesPath))).getSingleResult();
    }

    @Override
    public <T> Collection<T> getAll(Class<T> type, int startIdx, int count) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(type);
        Root<T> all = query.from(type);
        CriteriaQuery<T> select = query.select(all);
        return em.createQuery(select).setFirstResult(startIdx).setMaxResults(count).getResultList();
    }

    @Override
    public <T> Collection<T> getAll(Class<T> type) {
        return getAll(type, 0, Integer.MAX_VALUE);
    }

    @Override
    public Set<ApplicationGroup> getApplicationGroups() {
        return new HashSet<>(em.createQuery("from ApplicationGroup", ApplicationGroup.class).getResultList());
    }

    @Override
    public ApplicationGroup findApplicationGroupByName(String groupName) {
        String query = "select ag from ApplicationGroup ag where lower(ag.name)= :groupname";
        return getSingleResultOrNull(em.createQuery(query, ApplicationGroup.class).setParameter("groupname", groupName.toLowerCase()));

    }

    @Override
    public Optional<ApplicationGroup> findApplicationGroup(Collection<Application> applications) {
        ApplicationGroup foundAppGroup= null;
        for (Application application : applications) {
            ApplicationGroup appGroup = findApplicationGroup(application);
            if (appGroup == null ){
                log.debug("Application {} is not in an applicationGroup" , application );
                Optional.absent();
            }
            if (foundAppGroup != null  && !foundAppGroup.equals(appGroup)){
                log.debug("Application {} is in applicationGroup {}, expected it to be in {}" , application, appGroup, foundAppGroup );
                Optional.absent();
            }
            foundAppGroup=appGroup;
        }
        return Optional.fromNullable(foundAppGroup);
    }

    @Override
    public ApplicationGroup findApplicationGroup(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("Application can not be null");
        }

        String query = "select ag from ApplicationGroup ag inner join ag.applications a where a.id = :applicationid";
        return getSingleResultOrNull(em.createQuery(query, ApplicationGroup.class).setParameter("applicationid", application.getID()));
    }

}
