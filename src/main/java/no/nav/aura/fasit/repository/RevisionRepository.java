package no.nav.aura.fasit.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.order.AuditOrder;

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.AdditionalRevisionInfo;
import no.nav.aura.envconfig.model.ModelEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.*;

@Component
public class RevisionRepository {
    
    @PersistenceContext
    private EntityManager em;
       
       
    public <T extends ModelEntity> List<FasitRevision<T>> getRevisionsFor(Class<T> entityClass, Long entityId) {
        if (entityId == null) {
            return new ArrayList<>();
        }
        AuditReader auditReader = AuditReaderFactory.get(em);
        List<Object[]> revisions = auditReader.
                createQuery().
                forRevisionsOfEntity(entityClass, entityClass.getName(), false, true).
                add(AuditEntity.id().eq(entityId)).
                getResultList();

        List<FasitRevision<T>> entityRevisions = revisions.stream()
            .map(objects -> createFasitRevision(objects, entityClass))
            .collect(toList());
        return entityRevisions;
    }

    /**
     * Må være i egen metode pga en jvmbug som gir komileringsfeil med noen versjoner av java8 
     */
    private <T extends ModelEntity> FasitRevision<T> createFasitRevision(Object[] objects, Class<T> enityClass) {
        return new FasitRevision((AdditionalRevisionInfo<T>)objects[1], (ModelEntity)objects[0],(RevisionType)objects[2]);
    }
    
    public <T extends ModelEntity> Optional<T> getRevisionEntry(Class<T> entityClass, long entityId, long revision) {
        AuditReader auditReader = AuditReaderFactory.get(em);
        T historicEntity = auditReader.find(entityClass, entityId, revision);
        return Optional.ofNullable(historicEntity);
    }
    
    public <T extends ModelEntity> Long currentRevision(Class<T> entityClass, Long entityId) {
        AuditReader auditReader = AuditReaderFactory.get(em);
        Number revision = (Number)auditReader.createQuery()
                .forRevisionsOfEntity(entityClass, entityClass.getName(), false, true)
                .addProjection (AuditEntity.revisionNumber().max())
                .add(AuditEntity.id().eq(entityId))
                .getSingleResult();
        return revision.longValue();

    }


//    public <T extends ModelEntity> FasitRevision<T> getRevision(Class<T> entityClass, long entityId, long revision) {
//        AuditReader auditReader = AuditReaderFactory.get(em);
//        @SuppressWarnings("unchecked")
//        AdditionalRevisionInfo<T> revInfo = auditReader.findRevision(AdditionalRevisionInfo.class, revision);
//        T historicEntity = auditReader.find(entityClass, entityId, revision);
//        return new FasitRevision<T>(revInfo, historicEntity);
//    }
//
//    @SuppressWarnings({ "rawtypes", "unchecked" })
//    public List<AdditionalRevisionInfo<ModelEntity>> findHistory(Class<?> entityType, Date from, String authorId, long startIdx, int count, String orderBy, boolean sortAscending) {
//        CriteriaBuilder builder = em.getCriteriaBuilder();
//        CriteriaQuery<AdditionalRevisionInfo> query = em.getCriteriaBuilder().createQuery(AdditionalRevisionInfo.class);
//        Root<AdditionalRevisionInfo> root = query.from(AdditionalRevisionInfo.class);
//
//        List<Predicate> predicates = createHistoryPredicates(entityType, from, authorId, builder, root);
//
//        query.where(predicates.toArray(new Predicate[] {}));
//        Path<Object> orderByPath = root.get(orderBy);
//        query.orderBy(sortAscending ? builder.asc(orderByPath) : builder.desc(orderByPath));
//        // Cheating; generics in generics are not pretty in java
//        List resultList = em.createQuery(query).setMaxResults(count).setFirstResult((int) startIdx).getResultList();
//        return (List<AdditionalRevisionInfo<ModelEntity>>) resultList;
//    }
//
//    @SuppressWarnings({ "rawtypes" })
//    public long countHistory(Class<?> entityType, Date from, String authorId) {
//        CriteriaBuilder builder = em.getCriteriaBuilder();
//        CriteriaQuery<Long> query = builder.createQuery(Long.class);
//        Root<AdditionalRevisionInfo> root = query.from(AdditionalRevisionInfo.class);
//        List<Predicate> predicates = createHistoryPredicates(entityType, from, authorId, builder, root);
//
//        query.select(builder.count(root));
//        query.where(predicates.toArray(new Predicate[] {}));
//        return (Long) em.createQuery(query).getSingleResult();
//    }
//
//    @SuppressWarnings("rawtypes")
//    private List<Predicate> createHistoryPredicates(Class<?> entityType, Date from, String authorId, CriteriaBuilder builder, Root<AdditionalRevisionInfo> root) {
//        List<Predicate> predicates = new ArrayList<Predicate>();
//        if (entityType != null) {
//            predicates.add(builder.equal(root.get("modifiedEntityType"), entityType));
//        }
//        if (authorId != null) {
//            predicates.add(builder.equal(root.get("authorId"), authorId));
//        }
//        if (from != null) {
//            predicates.add(builder.greaterThanOrEqualTo(root.<Date> get("timestamp"), from));
//        }
//        return predicates;
//    }
//
//    @SuppressWarnings({ "unchecked" })
//    public <T> List<Tuple<T, RevisionType>> getEntitiesForRevision(Class<T> entityClass, long revision) {
//        AuditReader auditReader = AuditReaderFactory.get(em);
//        List<Object[]> resultList = auditReader.createQuery().forRevisionsOfEntity(entityClass, entityClass.getName(), false, true).add(AuditEntity.revisionNumber().eq(revision)).getResultList();
//        return resultList.stream()
//                .map(input -> Tuple.of((T) input[0], (RevisionType) input[2]))
//                .collect(Collectors.toList());
//                
//    }


}
