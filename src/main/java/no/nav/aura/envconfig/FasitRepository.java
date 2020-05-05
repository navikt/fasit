package no.nav.aura.envconfig;

import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.model.AdditionalRevisionInfo;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.util.Tuple;
import org.hibernate.envers.RevisionType;

import com.google.common.base.Optional;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface FasitRepository {

    <T extends ModelEntity> T store(T entity);

    void delete(ModelEntity entity);

    <T> T getById(Class<T> entityClass, long id);

    <T> long count(Class<T> entityClass);

    List<Resource> findResourcesByLikeAlias(Scope scope, ResourceType type, String alias, int first, int count, String sortByProperty, boolean sortAscending);

    List<Resource> findResourcesByExactAlias(Scope scope, ResourceType type, String alias);

    long findNrOfResources(Scope scope, ResourceType type, String alias);

    String getConnectionDescription();

    Set<Application> getApplications();

    Set<Application> getApplicationsNotInApplicationGroup();

    Application findApplicationByName(String applicationName);

    List<Environment> getEnvironments();

    Environment getEnvironmentBy(ApplicationInstance appinstance);

    Environment getEnvironmentBy(Node node);

    Environment findEnvironmentBy(String name);

    List<Environment> findEnvironmentsBy(EnvironmentClass environmentClass);

    Environment getEnvironmentBy(Cluster cluster);

    List<ApplicationInstance> findApplicationInstancesBy(Application application);

    @Nullable
    ApplicationInstance findApplicationInstanceByExposedResourceId(Long resourceId);

    Set<Cluster> findClustersBy(ApplicationGroup applicationGroup);

    Node findNodeBy(String hostName);

    List<Resource> findDuplicateProperties(Resource resource);

    List<Resource> findOverlappingResourceScope(Resource resource);

    List<Tuple<Long, RevisionType>> getRevisionsFor(Class<? extends ModelEntity> entityClass, Long entityId);

    <T extends ModelEntity> FasitRevision<T> getRevision(Class<T> entityClass, long entityId, long revision);

    <T> List<Tuple<T, RevisionType>> getEntitiesForRevision(Class<T> entityClass, long revision);

    Collection<ResourceReference> findFutureResourceReferencesBy(String resourceName, ResourceType resourceType);

    ApplicationInstance getApplicationInstanceBy(ResourceReference resourceReference);

    Set<ApplicationGroup> getApplicationGroups();

    ApplicationGroup findApplicationGroupByName(String groupName);

    Optional<ApplicationGroup> findApplicationGroup(Collection<Application> applications);

    ApplicationGroup findApplicationGroup(Application application);

    <T> Collection<T> getAll(Class<T> type, int startIdx, int count);

    <T> Collection<T> getAll(Class<T> type);

    List<AdditionalRevisionInfo<ModelEntity>> findHistory(Class<?> entityType, Date from, String authorId, long startIdx, int count, String orderBy, boolean sortAscending);

    long countHistory(Class<?> entityType, Date from, String authorId);

    void delete(ExposedServiceReference entity);
}
