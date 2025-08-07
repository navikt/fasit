package no.nav.aura.envconfig.filter;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HttpMetricFilterTest {

    private HttpMetricFilter filter = new HttpMetricFilter();

    @Test
    public void unknownUriReturnsNull() {
        assertNull(filter.mapToTimedService("/conf/v2/doesntexist"));
        assertNull(filter.mapToTimedService("/api/v2/doesntexist"));
    }

    @Test
    public void onlyContextRootReturnsNull() {
        assertNull(filter.mapToTimedService("/conf"));
        assertNull(filter.mapToTimedService("/conf/"));
        assertNull(filter.mapToTimedService("/api"));
        assertNull(filter.mapToTimedService("/api/"));
    }

    @Test
    public void returnsKnownUris() {
        // Environments
        assertThat(filter.mapToTimedService("/conf/environments"), is("/conf/environments"));
        assertThat(filter.mapToTimedService("/conf/environments/"), is("/conf/environments"));

        //Applicationinstances
        assertThat(filter.mapToTimedService("/conf/v1/applicationinstances"), is("/conf/v1/applicationinstances"));
        assertThat(filter.mapToTimedService("/conf/v1/applicationinstances/"), is("/conf/v1/applicationinstances"));

        //Applications
        assertThat(filter.mapToTimedService("/conf/applications"), is("/conf/applications"));
        assertThat(filter.mapToTimedService("/conf/applications/"), is("/conf/applications"));

        //Nodes
        assertThat(filter.mapToTimedService("/conf/nodes"), is("/conf/nodes"));
        assertThat(filter.mapToTimedService("/conf/nodes/"), is("/conf/nodes"));

        //Resources
        assertThat(filter.mapToTimedService("/conf/resources"), is("/conf/resources"));
        assertThat(filter.mapToTimedService("/conf/resources/"), is("/conf/resources"));

        // Environments
        assertThat(filter.mapToTimedService("/api/v2/environments"), is("/api/v2/environments"));
        assertThat(filter.mapToTimedService("/api/v2/environments/"), is("/api/v2/environments"));

        //Applicationinstances
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances"), is("/api/v2/applicationinstances"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/"), is("/api/v2/applicationinstances"));

        //Applications
        assertThat(filter.mapToTimedService("/api/v2/applications"), is("/api/v2/applications"));
        assertThat(filter.mapToTimedService("/api/v2/applications/"), is("/api/v2/applications"));

        //Nodes
        assertThat(filter.mapToTimedService("/api/v2/nodes"), is("/api/v2/nodes"));
        assertThat(filter.mapToTimedService("/api/v2/nodes/"), is("/api/v2/nodes"));

        //Resources
        assertThat(filter.mapToTimedService("/api/v2/resources"), is("/api/v2/resources"));
        assertThat(filter.mapToTimedService("/api/v2/resources/"), is("/api/v2/resources"));
    }

    @Test
    public void urisWithIdReturnsGeneric() {
        // Environments
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment"), is("/conf/environments/{name}"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/"), is("/conf/environments/{name}"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/applications"), is("/conf/environments/{name}/applications"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/applications/"), is("/conf/environments/{name}/applications"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/applications/myapp"), is("/conf/environments/{name}/applications/{appname}"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/applications/myapp/"), is("/conf/environments/{name}/applications/{appname}"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/applications/myapp/appconfig"), is("/conf/environments/{name}/applications/{appname}/appconfig"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/applications/myapp/appconfig/"), is("/conf/environments/{name}/applications/{appname}/appconfig"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/applications/myapp/verify"), is("/conf/environments/{name}/applications/{appname}/verify"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment/applications/myapp/verify/"), is("/conf/environments/{name}/applications/{appname}/verify"));
        assertThat(filter.mapToTimedService("/conf/environments/myapplication/somethingWeird"), is(nullValue()));

        //Application instances
        assertThat(filter.mapToTimedService("/conf/v1/environments/myenvironment/applications/myapp"), is("/conf/v1/environments/{envname}/applications/{appname}"));
        assertThat(filter.mapToTimedService("/conf/v1/environments/myenvironment/applications/myapp/"), is("/conf/v1/environments/{envname}/applications/{appname}"));
        assertThat(filter.mapToTimedService("/conf/v1/environments/myenvironment/applications/myapp/full"), is("/conf/v1/environments/{envname}/applications/{appname}/full"));
        assertThat(filter.mapToTimedService("/conf/v1/environments/myenvironment/applications/myapp/full/"), is("/conf/v1/environments/{envname}/applications/{appname}/full"));


        assertThat(filter.mapToTimedService("/conf/environments/myenvironment"), is("/conf/environments/{name}"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment"), is("/conf/environments/{name}"));
        assertThat(filter.mapToTimedService("/conf/environments/myenvironment"), is("/conf/environments/{name}"));

        // Applications
        assertThat(filter.mapToTimedService("/conf/applications/myapplication"), is("/conf/applications/{name}"));
        assertThat(filter.mapToTimedService("/conf/applications/myapplication/"), is("/conf/applications/{name}"));
        assertThat(filter.mapToTimedService("/conf/applications/myapplication/somethingWeird"), is(nullValue()));

        // Nodes
        assertThat(filter.mapToTimedService("/conf/nodes/mynode"), is("/conf/nodes/{name}"));
        assertThat(filter.mapToTimedService("/conf/nodes/mynode/"), is("/conf/nodes/{name}"));
        assertThat(filter.mapToTimedService("/conf/nodes/mynode/somethingWeird"), is(nullValue()));

        // Resources
        assertThat(filter.mapToTimedService("/conf/resources/bestmatch"), is("/conf/resources/bestmatch"));
        assertThat(filter.mapToTimedService("/conf/resources/bestmatch/"), is("/conf/resources/bestmatch"));

        assertThat(filter.mapToTimedService("/conf/resources/69"), is("/conf/resources/{id}"));
        assertThat(filter.mapToTimedService("/conf/resources/69/"), is("/conf/resources/{id}"));

        // Environments
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment"), is("/api/v2/environments/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/"), is("/api/v2/environments/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/revisions"), is("/api/v2/environments/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/revisions/"), is("/api/v2/environments/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/somethingWeird"), is(nullValue()));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/revisions/69"), is("/api/v2/environments/{name}/revisions/{revision}"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/revisions/69/"), is("/api/v2/environments/{name}/revisions/{revision}"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/clusters"), is("/api/v2/environments/{name}/clusters"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/clusters/"), is("/api/v2/environments/{name}/clusters"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/clusters/dasCluster"), is("/api/v2/environments/{name}/clusters/{clustername}"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/clusters/dasCluster/"), is("/api/v2/environments/{name}/clusters/{clustername}"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/clusters/dasCluster/revisions"), is("/api/v2/environments/{name}/clusters/{clustername}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/clusters/dasCluster/revisions/"), is("/api/v2/environments/{name}/clusters/{clustername}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/environments/myenvironment/clusters/dasCluster/revisions/69"), is("/api/v2/environments/{name}/clusters/{clustername}/revisions/{revision}"));

        // Applicationinstances
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication"), is("/api/v2/applicationinstances/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/"), is("/api/v2/applicationinstances/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/revisions"), is("/api/v2/applicationinstances/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/revisions/"), is("/api/v2/applicationinstances/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/appconfig"), is("/api/v2/applicationinstances/{name}/appconfig"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/appconfig/"), is("/api/v2/applicationinstances/{name}/appconfig"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/somethingWeird"), is(nullValue()));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/revisions/69"), is("/api/v2/applicationinstances/{name}/revisions/{revision}"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/revisions/69/"), is("/api/v2/applicationinstances/{name}/revisions/{revision}"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/revisions/69/appconfig"), is("/api/v2/applicationinstances/{name}/revisions/{revision}/appconfig"));
        assertThat(filter.mapToTimedService("/api/v2/applicationinstances/myapplication/revisions/69/appconfig/"), is("/api/v2/applicationinstances/{name}/revisions/{revision}/appconfig"));

        // Applications
        assertThat(filter.mapToTimedService("/api/v2/applications/myapplication"), is("/api/v2/applications/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/applications/myapplication/"), is("/api/v2/applications/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/applications/myapplication/revisions"), is("/api/v2/applications/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/applications/myapplication/revisions/"), is("/api/v2/applications/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/applications/myapplication/somethingWeird"), is(nullValue()));
        assertThat(filter.mapToTimedService("/api/v2/applications/myapplication/revisions/69"), is("/api/v2/applications/{name}/revisions/{revision}"));
        assertThat(filter.mapToTimedService("/api/v2/applications/myapplication/revisions/69/"), is("/api/v2/applications/{name}/revisions/{revision}"));

        // Nodes
        assertThat(filter.mapToTimedService("/api/v2/nodes/types"), is("/api/v2/nodes/types"));
        assertThat(filter.mapToTimedService("/api/v2/nodes/types/"), is("/api/v2/nodes/types"));
        assertThat(filter.mapToTimedService("/api/v2/nodes/mynode"), is("/api/v2/nodes/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/nodes/mynode/"), is("/api/v2/nodes/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/nodes/mynode/revisions"), is("/api/v2/nodes/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/nodes/mynode/revisions/"), is("/api/v2/nodes/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/nodes/mynode/somethingWeird"), is(nullValue()));
        assertThat(filter.mapToTimedService("/api/v2/nodes/mynode/revisions/69"), is("/api/v2/nodes/{name}/revisions/{revision}"));
        assertThat(filter.mapToTimedService("/api/v2/nodes/mynode/revisions/69/"), is("/api/v2/nodes/{name}/revisions/{revision}"));

        // Resources
        assertThat(filter.mapToTimedService("/api/v2/resources/types"), is("/api/v2/resources/types"));
        assertThat(filter.mapToTimedService("/api/v2/resources/types/"), is("/api/v2/resources/types"));
        assertThat(filter.mapToTimedService("/api/v2/resources/types/mytype"), is("/api/v2/resources/types/{type}"));
        assertThat(filter.mapToTimedService("/api/v2/resources/types/mytype/"), is("/api/v2/resources/types/{type}"));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource"), is("/api/v2/resources/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource/"), is("/api/v2/resources/{name}"));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource/file/myfile"), is("/api/v2/resources/{name}/file/{file}"));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource/file/myfile/"), is("/api/v2/resources/{name}/file/{file}"));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource/revisions"), is("/api/v2/resources/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource/revisions/"), is("/api/v2/resources/{name}/revisions"));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource/somethingWeird"), is(nullValue()));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource/revisions/69"), is("/api/v2/resources/{name}/revisions/{revision}"));
        assertThat(filter.mapToTimedService("/api/v2/resources/myresource/revisions/69/"), is("/api/v2/resources/{name}/revisions/{revision}"));

        //Search
        assertThat(filter.mapToTimedService("/api/v1/navsearch?q=searchstring"), is("/api/v1/navsearch"));
        assertThat(filter.mapToTimedService("/api/v1/navsearch?q=searchstring&maxCount=69"), is("/api/v1/navsearch"));
        assertThat(filter.mapToTimedService("/api/v1/search?q=searchstring"), is("/api/v1/search"));
        assertThat(filter.mapToTimedService("/api/v1/search?q=searchstring&maxCount=69"), is("/api/v1/search"));
    }
}