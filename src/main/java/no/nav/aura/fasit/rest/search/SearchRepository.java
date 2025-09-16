package no.nav.aura.fasit.rest.search;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.fasit.rest.model.LifecyclePayload;
import no.nav.aura.fasit.rest.model.SearchResultPayload;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;

import java.net.URI;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static no.nav.aura.fasit.rest.search.SearchRepository.SearchType.NAVIGATION;
import static no.nav.aura.fasit.rest.search.SearchRepository.SearchType.SEARCH;
import static no.nav.aura.fasit.rest.search.SearchResultType.*;

@Component
public class SearchRepository {

    enum SearchType {
        NAVIGATION, SEARCH
    }

    @PersistenceContext
    private EntityManager em;


    public Set<SearchResultPayload> navigationSearch(String searchString, Integer maxCount, URI baseUri) {
        return findMatches(searchString, maxCount, NAVIGATION, SearchResultType.ALL, baseUri);
    }

    public Set<SearchResultPayload> search(String searchString, Integer maxCount, SearchResultType typeFilter, URI baseUri) {
        Set<SearchResultPayload> searchResults = new HashSet();

        if (NumberUtils.isNumber(searchString)) {
            Long id = Long.valueOf(searchString);
            if(typeFilter.equals(ALL) || typeFilter.equals(RESOURCE)) {
                getById(Resource.class, id).ifPresent(resource -> searchResults.add(toSearchResult(resource, SEARCH, RESOURCE, baseUri)));
            }

            if(typeFilter.equals(ALL) || typeFilter.equals(INSTANCE)) {
                getById(ApplicationInstance.class, id).ifPresent(instance -> searchResults.add(toSearchResult(instance, SEARCH, INSTANCE, baseUri)));
            }
        }



         searchResults.addAll(findMatches(searchString, maxCount, SearchType.SEARCH, typeFilter, baseUri));

        if (searchResults.size() < maxCount && (typeFilter.equals(ALL) || typeFilter.equals(RESOURCE))) {
            searchResults.addAll(searchForResourceProperties(searchString, maxCount - searchResults.size(), baseUri));
        }

        if (searchResults.size() < maxCount && (typeFilter.equals(ALL) || typeFilter.equals(APPCONFIG))) {
            searchResults.addAll(searchInAppConfig(searchString, maxCount - searchResults.size(), baseUri));
        }

        return searchResults;
    }

    private Set<SearchResultPayload> findMatches(String searchString, Integer maxCount, SearchType searchType, SearchResultType typeFilter, URI baseUri) {
        List<SearchResultPayload> entities = new ArrayList<>();
        boolean noFilter = typeFilter.equals(ALL);

        if ((noFilter || typeFilter.equals(INSTANCE)) && hasMultipleWords(searchString)) {
            return searchInstances(searchString, searchType, baseUri);
        }

        if (noFilter || typeFilter.equals(ENVIRONMENT)) {
            entities.addAll(toSearchResults(findMatches(Environment.class, "name", searchString, maxCount), searchType, ENVIRONMENT, baseUri));
        }

        if (entities.size() <= maxCount && (noFilter || typeFilter.equals(APPLICATION) || typeFilter.equals(INSTANCE))) {
            List<? extends DeleteableEntity> applications = findMatches(Application.class, "name", searchString, maxCount - entities.size());

            if (noFilter || typeFilter.equals(APPLICATION)) {
                entities.addAll(toSearchResults(applications, searchType, APPLICATION, baseUri));
            }


            if (entities.size() <= maxCount && (noFilter || typeFilter.equals(INSTANCE))) {
                List<ApplicationInstance> applicationInstancesByAplication = findApplicationInstancesByAplication(applications);
                entities.addAll(toSearchResults(applicationInstancesByAplication, searchType, INSTANCE, baseUri));
            }
        }

        if (entities.size() <= maxCount && (noFilter || typeFilter.equals(NODE))) {
            List<? extends DeleteableEntity> nodes = findMatches(Node.class, "hostname", searchString, maxCount - entities.size());
            entities.addAll(toSearchResults(nodes, searchType, NODE, baseUri));
        }

        if (entities.size() <= maxCount && (noFilter || typeFilter.equals(RESOURCE))) {
            List<? extends DeleteableEntity> resources = findMatches(Resource.class, "alias", searchString, maxCount - entities.size());

            entities.addAll(toSearchResults(resources, searchType, RESOURCE, baseUri));
        }

        if (entities.size() <= maxCount && (noFilter || typeFilter.equals(CLUSTER))) {
            List<? extends DeleteableEntity> clusters = findMatches(Cluster.class, "name", searchString, maxCount - entities.size());
            entities.addAll(toSearchResults(clusters, searchType, CLUSTER, baseUri));
        }

        return new HashSet(entities.subList(0, Math.min(entities.size(), maxCount)));
    }

    private Set<SearchResultPayload> searchInstances(String searchString, SearchType searchType, URI baseUri) {
        String environmentName = searchString.split(" ")[0];
        String applicationName = searchString.split(" ")[1];
        Environment environment = getEnvironmentBy(environmentName);

        if (environment == null) {
            return new HashSet();
        }

        List<ApplicationInstance> actualInstances = environment.getApplicationInstances().stream()
                .filter(instance -> instance.getApplication().getName().contains(applicationName))
                .collect(toList());

        return toSearchResults(actualInstances, searchType, INSTANCE, baseUri);
    }

    public Set<SearchResultPayload> toSearchResults(List<? extends DeleteableEntity> results, SearchType type, SearchResultType searchResultType, URI baseUri) {
        Set<SearchResultPayload> searchResults = new HashSet<>();

        for (DeleteableEntity entity : results) {
            searchResults.add(toSearchResult(entity, type, searchResultType, baseUri));
        }

        return searchResults;
    }


    private SearchResultPayload toSearchResult(DeleteableEntity entity, SearchType type, SearchResultType searchResultType, URI baseUri) {
        String searchResultText;

        if (searchResultType.equals(CLUSTER)) {
            searchResultText = getEnvironmentBy((Cluster) entity).getName();
        } else if (searchResultType.equals(NODE)) {
            searchResultText = getEnvironmentBy((Node) entity) + " | " + entity.getInfo();
        } else if (type.equals(NAVIGATION) && searchResultType.equals(INSTANCE)) {
            Cluster cluster = ((ApplicationInstance) entity).getCluster();
            searchResultText = getEnvironmentBy(cluster).getName();
        } else {
            searchResultText = entity.getInfo();
        }

        SearchResultPayload resultPayload = new SearchResultPayload();
        resultPayload.id = entity.getID();
        resultPayload.name = entity.getName();
        resultPayload.type = searchResultType.toString();
        resultPayload.info = searchResultText;
        resultPayload.link = generateLink(baseUri, searchResultType, entity);

        if (type.equals(SEARCH)) {
            resultPayload.detailedInfo = entity.getEnityProperties();
            LifecyclePayload lifecyclePayload = new LifecyclePayload();
            lifecyclePayload.status = entity.getLifeCycleStatus();
            resultPayload.lifecycle = lifecyclePayload;

            if (searchResultType.equals(APPCONFIG)) {
                resultPayload.detailedInfo.put("appConfig", ((ApplicationInstance) entity).getAppconfigXml());
            }

            if(searchResultType.equals(INSTANCE) || searchResultType.equals(APPCONFIG)) {
                Cluster cluster = ((ApplicationInstance) entity).getCluster();
                resultPayload.detailedInfo.put("environment", getEnvironmentBy(cluster).getName());
            }
            Optional.ofNullable(entity.getUpdated()).ifPresent(updated -> resultPayload.lastChange = updated.getMillis());
        }
        return resultPayload;
    }

    private URI generateLink(URI baseUri, SearchResultType searchResultType, DeleteableEntity entity) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUri);

        switch (searchResultType) {
            case APPLICATION:
                return builder.path("/api/v2/applications/{name}")
                        .buildAndExpand(entity.getName())
                        .toUri();
            case NODE:
                return builder.path("/api/v2/nodes/{name}")
                        .buildAndExpand(entity.getName())
                        .toUri();
            case APPCONFIG:
            case INSTANCE:
                return builder.path("/api/v2/applicationinstances/{id}")
                        .buildAndExpand(entity.getID())
                        .toUri();
            case RESOURCE:
                return builder.path("/api/v2/resources/{id}")
                        .buildAndExpand(entity.getID())
                        .toUri();
            case ENVIRONMENT:
                return builder.path("/api/v2/environments/{name}")
                        .buildAndExpand(entity.getName())
                        .toUri();
            case CLUSTER:
                Cluster cluster = (Cluster) entity;
                String environment = getEnvironmentBy(cluster).getName();
                return builder.path("/api/v2/environments/{env}/clusters/{name}")
                        .buildAndExpand(environment, cluster.getName())
                        .toUri();
        }
        return baseUri;
    }


    private static boolean hasMultipleWords(String string) {
        return string.split(" ").length > 1;
    }

    public String getEnvironmentBy(Node node) {
        List<Environment> result = em.createQuery("select ep from Environment ep where :node MEMBER OF ep.nodes", Environment.class).setParameter("node", node).getResultList();
        if (!result.isEmpty()) {
            return result.get(0).getName();
        } else return "";
    }

    public <T> Optional<T> getById(Class<T> entityClass, long id) {
        T entity = em.find(entityClass, id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(entity);
    }

    public Environment getEnvironmentBy(Cluster cluster) {
        String query = "select e from Environment e join e.clusters clusters  where clusters.id = :clusterId";
        return em.createQuery(query, Environment.class).setParameter("clusterId", cluster.getID()).getSingleResult();
    }

    public Environment getEnvironmentBy(String name) {
        String query = "select e from Environment e where lower(e.name)=:name";
        List<Environment> environmentResult = em.createQuery(query, Environment.class).setParameter("name", name).getResultList();
        if (environmentResult.isEmpty()) {
            return null;
        }
        return environmentResult.get(0);
    }

    private List<? extends DeleteableEntity> findMatches(Class<? extends DeleteableEntity> entityClass, String fieldName, String search, int maxCount) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<? extends DeleteableEntity> query = builder.createQuery(entityClass);
        Path<String> namePath = query.from(entityClass).get(fieldName);

        return em.createQuery(query.where(builder.like(builder.lower(namePath), "%" + search.toLowerCase() + "%"))).setMaxResults(maxCount).getResultList();
    }

    public List<ApplicationInstance> findApplicationInstancesByAplication(List<? extends DeleteableEntity> applications) {
        List<ApplicationInstance> instances = new ArrayList();
        for (DeleteableEntity application : applications) {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<ApplicationInstance> query = builder.createQuery(ApplicationInstance.class);
            Root<ApplicationInstance> from = query.from(ApplicationInstance.class);
            List<ApplicationInstance> matchingAppInstances = em.createQuery(query.where(builder.equal(from.get("application"), application))).getResultList();
            instances.addAll(matchingAppInstances);
        }
        return instances;
    }


    protected Set<SearchResultPayload> searchInAppConfig(String queryString, int maxResults, URI baseUri) {
        Query query = em.createNativeQuery("select DISTINCT * from applicationinstance where dbms_lob.instr(lower(appconfigxml),'" + queryString.toLowerCase() + "')> 0", ApplicationInstance.class);
        query.setMaxResults(maxResults);
        List<ApplicationInstance> results = query.getResultList();

        return toSearchResults(results, SearchType.SEARCH, APPCONFIG, baseUri);
    }

    protected Set<SearchResultPayload> searchForResourceProperties(String queryString, int maxResults, URI baseUri) {

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Resource> rooQuery = builder.createQuery(Resource.class);
        Root<Resource> resourceRoot = rooQuery.from(Resource.class);
        MapJoin<Resource, String, String> properties = resourceRoot.joinMap("properties", JoinType.INNER);
        TypedQuery<Resource> query = em.createQuery(rooQuery.where(builder.like(builder.lower(properties.value()), "%" + queryString.toLowerCase() + "%")).distinct(true));
        query.setMaxResults(maxResults);

        List<Resource> results = query.getResultList();

        return toSearchResults(results, SearchType.SEARCH, RESOURCE, baseUri);
    }
}