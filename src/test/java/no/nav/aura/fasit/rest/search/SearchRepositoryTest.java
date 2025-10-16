package no.nav.aura.fasit.rest.search;

import static no.nav.aura.fasit.rest.search.SearchResultType.APPLICATION;
import static no.nav.aura.fasit.rest.search.SearchResultType.CLUSTER;
import static no.nav.aura.fasit.rest.search.SearchResultType.ENVIRONMENT;
import static no.nav.aura.fasit.rest.search.SearchResultType.INSTANCE;
import static no.nav.aura.fasit.rest.search.SearchResultType.NODE;
import static no.nav.aura.fasit.rest.search.SearchResultType.RESOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.spring.SpringUnitTestConfig;
import no.nav.aura.fasit.rest.model.SearchResultPayload;

@SpringJUnitConfig(classes = {SpringUnitTestConfig.class})
@Transactional
@Rollback
public class SearchRepositoryTest {

    @Autowired
    private FasitRepository repository;
    @Autowired
    private SearchRepository searchRepository;
    private URI baseUri;
    private Resource someResource;
    private List<Long> storedAppInstanceIds = new ArrayList();

    public SearchRepositoryTest() throws URISyntaxException {
    }

    @BeforeEach
    public void setup() throws URISyntaxException {
        baseUri  = new URI("somebaseuri.con");
        Environment someEnvironment = new Environment("someEnvironment", EnvironmentClass.u);
        Environment someMoreEnvironment = new Environment("someMoreEnvironment", EnvironmentClass.u);
        Cluster someCluster = repository.store(new Cluster("someCluster", Domain.Adeo));
        Cluster notDeployToCluster = repository.store(new Cluster("notDeployedToCluster", Domain.Devillo));
        notDeployToCluster.addApplication(repository.store(new Application("notDeployedApplication")));
        someEnvironment.addCluster(notDeployToCluster);
        Node someNode = new Node("someNode", "", "");
        Node differentNode = new Node("newNode", "", "");
        Application someApplication = repository.store(new Application("someApplication"));
        Application otherApplication = repository.store(new Application("otherApplication"));
        someCluster.addApplication(someApplication);
        someCluster.addApplication(otherApplication);
        someCluster.addNode(someNode);
        someEnvironment.addCluster(someCluster);
        someEnvironment.addNode(someNode);
        repository.store(differentNode);
        repository.store(someEnvironment);
        repository.store(someMoreEnvironment);



        someResource = new Resource("someResource", ResourceType.BaseUrl, new Scope(EnvironmentClass.p));
        someResource.putProperty("url", "someUrl");
        someResource = repository.store(someResource);

        Resource anotherResource = new Resource("otherResource", ResourceType.Channel, new Scope(EnvironmentClass.t));
        anotherResource.putProperty("name", "someChannel");
        anotherResource.putProperty("queueManager", "queuemanagerone");
        repository.store(anotherResource);

        Set<ApplicationInstance> applicationInstances = someCluster.getApplicationInstances();
        for (ApplicationInstance applicationInstance : applicationInstances) {
            applicationInstance.setVersion("69");
            applicationInstance.setAppconfigXml("this is my appconfig. There are many like it, but this one is mine");
            ApplicationInstance savedAppInstance = repository.store(applicationInstance);
            storedAppInstanceIds.add(savedAppInstance.getID());
        }
    }
    

    @Test
    public void matches() throws URISyntaxException {
        baseUri = new URI("baseurl.com");
        Set<SearchResultPayload> matchesSome = searchRepository.navigationSearch("some", 10, baseUri);
        Set<SearchResultPayload> matchesOther = searchRepository.navigationSearch("other", 10, baseUri);

        assertEquals(7, matchesSome.size(), "matches env, instance, cluster, app, resource withe some");
        assertEquals(3, matchesOther.size(), "matches env, instance, cluster, app, resource withe other");
        assertEquals(1, searchRepository.navigationSearch("someE", 10, baseUri).size(), "matches environment");
        assertEquals(2, searchRepository.navigationSearch("someA", 10, baseUri).size(), "matches application and appInstance");
        assertEquals(2, searchRepository.navigationSearch("node", 10, baseUri).size(), "matches node");
    }

    @Test
    public void findById() {
        Set<SearchResultPayload> matchesResourceId = searchRepository.search(someResource.getID().toString(), 5, RESOURCE, baseUri);
        Set<SearchResultPayload> matchesAppInstanceId = searchRepository.search(storedAppInstanceIds.get(0).toString(), 5, INSTANCE, baseUri);
        assertEquals(1, matchesResourceId.size(), "find resource by id");
        assertEquals(1, matchesAppInstanceId.size(), "find applicationInstance by id");

    }


    @Test
    public void applicationInstancesThatAreNotYetDeployedAreAlsoReturnedInSearchResults() {
        Set<SearchResultPayload> search = searchRepository.search("someenvironment notDeployedApplication", 5, INSTANCE, baseUri);
        assertEquals(1, search.size(), "matches application instance that is not yet deployed");
        assertEquals("Not deployed", search.iterator().next().detailedInfo.get("version"), "no version number is set");
    }

    @Test
    public void filterSearch() throws URISyntaxException {
        baseUri = new URI("baseurl.com");
        Set<SearchResultPayload> matchesSomeResources = searchRepository.search("some", 5, RESOURCE, baseUri);
        Set<SearchResultPayload> matchesSomeEnvironment = searchRepository.search("some", 5, ENVIRONMENT, baseUri);
        Set<SearchResultPayload> matchesSomeCluster = searchRepository.search("some", 5, CLUSTER, baseUri);
        Set<SearchResultPayload> matchesSomeApplication = searchRepository.search("some", 5, APPLICATION, baseUri);
        Set<SearchResultPayload> matchesSomeNode = searchRepository.search("some", 5, NODE, baseUri);

        Set<SearchResultPayload> matchesOtherApplication = searchRepository.search("other", 5, APPLICATION, baseUri);
        Set<SearchResultPayload> matchesOtherResource = searchRepository.search("other", 5, RESOURCE, baseUri);

        Set<SearchResultPayload> matchesSomeApplicationInstance = searchRepository.search("some", 5, INSTANCE, baseUri);


        assertEquals(2, matchesSomeResources.size(), "matches resources with some in properties or alias");
        assertEquals(2, matchesSomeEnvironment.size(), "matches environment with some");
        assertEquals(1, matchesSomeCluster.size(), "matches cluster with some");
        assertEquals(1, matchesSomeApplication.size(), "matches application with some");
        assertEquals(1, matchesSomeNode.size(), "matches node with some");
        assertEquals(1, matchesSomeApplicationInstance.size(), "matches instance with appname containing some");

        assertEquals(1, matchesOtherApplication.size(), "matches application with other");
        assertEquals(1, matchesOtherResource.size(), "matches resource with other");
    }


    @Test
    public void likeSearchInContentOfResource() {
        assertThat(searchRepository.searchForResourceProperties("some", 10, baseUri).size(), is(2));
    }

    @Test
    public void whenSearchMatchesBothAliasAndPropertiesInTheSameResourceOnlyUniqueResultsAreReturned() {
        Set<SearchResultPayload> resourcesMatchingSome = searchRepository.search("some", 10, RESOURCE, baseUri);
        assertThat("All IDs are uniqe in search results", resourcesMatchingSome.stream().map(result -> result.id).distinct().count(), is(2L));
        assertThat(resourcesMatchingSome.size(), is(2) );
    }

    @Test
    public void findsResultsForDifferentEntityTypes() {
        assertThat("it finds five results", searchRepository.navigationSearch("some", 10, baseUri).size(), is(7));
    }

    @Test
    public void capsSearchResultsToCorrectSize() {
        assertThat("it finds two results", searchRepository.navigationSearch("SoMe", 2, baseUri).size(), is(2));
    }

    @Test
    public void findsInstanceWhenProvidingMultipleSearchWords() {
        Set<SearchResultPayload> matches = searchRepository.navigationSearch("someenvironment s", 10, baseUri);
        assertThat("finds only one", matches.size(), is(1));
        assertThat("it finds instance", matches.iterator().next().type, is("instance"));
        assertThat("it finds correct instance", matches.iterator().next().name, is("someApplication:69"));
    }

    @Test
    public void nonExistingEnvironmentYieldsEmptySearchResultWhenProvidingMultipleWords() {
        Set<SearchResultPayload> matches = searchRepository.navigationSearch("nonexisting blahblah", 10, baseUri);
        assertThat("empty search result", matches.size(), is(0));
    }
}