package no.nav.aura.envconfig.client;

import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import no.nav.aura.appconfig.Application;
import no.nav.aura.envconfig.client.DomainDO.EnvClass;
import no.nav.aura.envconfig.client.rest.PropertyElement;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.fasit.client.model.RegisterApplicationInstancePayload;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FasitRestClient {

    private URI baseUrl;
    private HttpClient httpClient;
    private static final Logger log = LoggerFactory.getLogger(FasitRestClient.class);
    private Map<URI, Object> cache = new HashMap<URI, Object>();
    private String onBehalfOf;
    private boolean useCache = true;

    public FasitRestClient(String baseUrl, String username, String password) {
        this.baseUrl = UriBuilder.fromUri(baseUrl).build();

        Credentials credentials = new UsernamePasswordCredentials(username, password);
        ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();
        // The number of concurrent requests allowed, default is two. Set to 1 for now, realizing only thread safety, but no
        // concurrency.
        connectionManager.setDefaultMaxPerRoute(1);
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient(connectionManager);
        defaultHttpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
        this.httpClient = defaultHttpClient;

        log.info("using rest based envconfig client with url : {} and user {}", baseUrl, username);
    }
  
    public UriBuilder getBaseUrl() {
        return UriBuilder.fromUri(baseUrl);
    }

    public ApplicationInstanceDO getApplicationInstance(String environment, String appName) {
        URI url = getBaseUrl().path("environments/{env}/applications/{app}").build(environment, appName);
        ApplicationInstanceDO appInstance = get(url, ApplicationInstanceDO.class);
        return appInstance;
    }

    public Collection<EnvironmentDO> getEnvironments() {
        URI url = getBaseUrl().path("environments").build();
        EnvironmentDO[] environments = get(url, EnvironmentDO[].class);
        return Arrays.asList(environments);
    }

    public Collection<ApplicationInstanceDO> getApplicationInstances(String environment) {
        URI url = getBaseUrl().path("environments/{env}/applications").build(environment);
        ApplicationInstanceDO[] instances = get(url, ApplicationInstanceDO[].class);
        return Arrays.asList(instances);
    }

    public String getSecret(URI url) {
        try {
            if (cache.containsKey(url)) {
                log.debug("Fetching {} from cache", url);
                return (String) cache.get(url);
            }
            log.debug("Calling url {}", url);
            ClientRequest client = createClientRequest(url.toString());
            client.header("showsecret", true);
            ClientResponse<String> response = client.get(String.class);
            checkResponse(response, url);
            String result = response.getEntity();
            putInCache(url, result);
            return result;
        } catch (Exception e) {
            throw rethrow(e);
        }

    }

    public int getNodeCount(String environment, String applicationName) {
        try {
            int nodeCount = getApplicationInstance(environment, applicationName).getCluster().getNodes().length;
            return nodeCount;
        } catch (IllegalArgumentException e) {
            return 0;
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public InputStream getFile(URI uri) {
        return get(uri, InputStream.class);
    }

    /** Find resources matching given scope */
    public Collection<ResourceElement> findResources(EnvClass envClass, String environment, DomainDO domain, String appName, ResourceTypeDO type, String alias) {
        URI url = buildResourceQuery(envClass, environment, domain, appName, type, alias, false, false);
        ResourceElement[] resources = get(url, ResourceElement[].class);
        return Arrays.asList(resources);
    }

    public ResourceElement findBestMatchingResource(EnvClass envClass, String environment, DomainDO domain, String appName, ResourceTypeDO type, String alias) {
        URI url = buildResourceQuery(envClass, environment, domain, appName, type, alias, true, false);
        ResourceElement[] resourceElement = get(url, ResourceElement[].class);
        if (resourceElement.length != 1) {
            throw new RuntimeException("Unable to find a single match (found " + resourceElement.length + "), not sure what to do");
        }
        return resourceElement[0];
    }

    public URI buildResourceQuery(EnvClass envClass, String environment, DomainDO domain, String appName, ResourceTypeDO type, String alias, Boolean bestMatch, Boolean usage) {
        UriBuilder uribuilder = getBaseUrl().path("resources");
        if (envClass != null) {
            uribuilder.queryParam("envClass", envClass);
        }
        if (environment != null) {
            uribuilder.queryParam("envName", environment);
        }
        if (domain != null) {
            uribuilder.queryParam("domain", domain.getFqn());
        }
        if (appName != null) {
            uribuilder.queryParam("app", appName);
        }
        if (type != null) {
            uribuilder.queryParam("type", type);
        }
        if (alias != null) {
            uribuilder.queryParam("alias", alias);
        }
        if(bestMatch !=null){
            uribuilder.queryParam("bestmatch", bestMatch);
        }
        if(usage !=null){
            uribuilder.queryParam("usage", usage);
        }

        URI url = uribuilder.build();
        log.debug("REST URL " + url);
        return url;
    }

    public boolean resourceExists(EnvClass envClass, String environment, DomainDO domain, String appName, ResourceTypeDO type, String alias) {
        return !findResources(envClass, environment, domain, appName, type, alias).isEmpty();
    }

    /** Find the best matching resource given a full scope */
    public ResourceElement getResource(String environment, String alias, ResourceTypeDO type, DomainDO domain, String appName) {
        URI url = getBaseUrl().path("resources/bestmatch").queryParam("envName", environment).queryParam("domain", domain.getFqn()).queryParam("type", type)
                .queryParam("alias", alias).queryParam("app", appName).build();
        log.debug("REST URL " + url);
        ResourceElement resource = get(url, ResourceElement.class);
        return resource;
    }

    public ResourceElement getResourceById(long resourceId) {
        URI url = getBaseUrl().path("resources/" + resourceId).build();
        log.debug("REST URL " + url);
        ResourceElement resource = get(url, ResourceElement.class);
        return resource;
    }

    public ApplicationDO getApplicationInfo(String appName) {
        URI url = getBaseUrl().path("applications/{appname}").build(appName);
        return get(url, ApplicationDO.class);
    }

    public ApplicationGroupDO getApplicationGroup(String name) {
        URI url = getBaseUrl().path("applicationGroups/{name}").build(name);
        return get(url, ApplicationGroupDO.class);
    }

    public Response registerApplication(RegisterApplicationInstancePayload payload, String comment) {
        URI url = getBaseUrl().path("/v1/applicationinstances").build();
        log.debug("Registering new application instance to endpoint {} with payload {}", url, payload.toJson());
        return post(url, payload.toJson(), comment, MediaType.APPLICATION_JSON);
    }


    public Response undeployApplication(String environmentName, String applicationName, String comment) {
        URI url = getBaseUrl().path("environments/{env}/applications/{app}").build(environmentName, applicationName);
        log.debug("Undeploying application {} on {} ", applicationName, url);
        return delete(url, comment);
    }

    public Response verifyApplication(String environment, String applicationName, Application application) {
        URI url = getBaseUrl().path("environments/{env}/applications/{app}/verify").build(environment, applicationName);
        log.debug("Verify application {} on {} ", applicationName, url);
        return put(url, application, null);
    }

    public NodeDO registerNode(NodeDO nodeDO, String comment) {
        URI uri = withComment(getBaseUrl().path("nodes"), comment).build();
        ClientRequest client = createClientRequest(uri.toString()).body(MediaType.APPLICATION_XML, nodeDO);
        try {
            ClientResponse<NodeDO> put = client.put(NodeDO.class);
            checkResponse(put, uri);
            return put.getEntity();
        } catch (Exception e) {
            log.warn("unable to register node", e);
            throw rethrow(e);
        }
    }

    /**
     * Oppdaterer ett node objekt.
     * 
     * @param nodeDO
     * @param comment
     * @return
     */
    public Response updateNode(NodeDO nodeDO, String comment) {
        URI uri = getBaseUrl().path("nodes").path(nodeDO.getHostname()).build();
        log.debug("Updating node on url", uri);
        return post(uri, nodeDO, comment);
    }

    public Response deleteResource(long id, String comment) {
        URI url = getBaseUrl().path("resources/{id}").build(id);
        return delete(url, comment);
    }

    public ResourceElement updateResource(long id, ResourceElement resource, String comment) {
        MultipartFormDataOutput data = createFormData(resource);
        return executeMultipart("POST", "resources/" + id, data, comment, ResourceElement.class);
    }

    /**
     * NB: Not implemented for file resources
     * 
     * @return
     */
    public ResourceElement registerResource(ResourceElement resource, String comment) {
        MultipartFormDataOutput data = createFormData(resource);
        return executeMultipart("PUT", "resources", data, comment, ResourceElement.class);
    }

    /**
     * Eksempel på data input<br/>
     * <code>
     *  MultipartFormDataOutput data = new MultipartFormDataOutput();<br/>
     *   data.addFormData("alias", "mintjeneste", MediaType.TEXT_PLAIN_TYPE);<br/>
     *   data.addFormData("scope.environmentclass", "u", MediaType.TEXT_PLAIN_TYPE);<br/>
     *   data.addFormData("scope.environmentname", "myTestEnv", MediaType.TEXT_PLAIN_TYPE);<br/>
     *   data.addFormData("scope.domain", "devillo.no", MediaType.TEXT_PLAIN_TYPE);<br/>
     *   data.addFormData("scope.application", "myApp", MediaType.TEXT_PLAIN_TYPE);<br/>
     *   data.addFormData("type", ResourceTypeDO.Certificate, MediaType.TEXT_PLAIN_TYPE);<br/>
     * <br/>
     *   data.addFormData("keystorealias", "app-key", MediaType.TEXT_PLAIN_TYPE);<br/>
     * </code>
     * 
     * @param method
     *            POST eller PUT
     * @param path
     *            i tillegg til baseurl til fasit ressurs
     * @param data
     *            multipart data
     * @param comment
     *            kommentar til fasit
     * @param responseClass
     * @return
     */

    public <T> T executeMultipart(String method, String path, MultipartFormDataOutput data, String comment, Class<T> responseClass) {
        URI url = withComment(getBaseUrl().path(path), comment).build();
        ClientRequest client = createClientRequest(url.toString()).body(MediaType.MULTIPART_FORM_DATA, data);
        try {
            log.debug("Sending multipart to {} with method {} ", url, method);
            ClientResponse<T> response = null;
            if ("PUT".equals(method)) {
                response = client.put(responseClass);
            } else if ("POST".equals(method)) {
                response = client.post(responseClass);
            } else {
                throw new IllegalArgumentException("Expected HTTP method POST or PUT. Got " + method);
            }
            checkResponse(response, url);
            return response.getEntity();
        } catch (Exception e) {
            log.warn("unable to register resource", e);
            throw rethrow(e);
        }
    }

    private MultipartFormDataOutput createFormData(ResourceElement resource) {
        MultipartFormDataOutput data = new MultipartFormDataOutput();
        Map<String, String> fields = new HashMap<>();
        fields.put("alias", resource.getAlias());
        fields.put("type", resource.getType().name());
        fields.put("scope.environmentclass", resource.getEnvironmentClass());
        fields.put("scope.domain", resource.getDomain() != null ? resource.getDomain().getFqn() : null);
        fields.put("scope.environmentname", resource.getEnvironmentName());
        fields.put("scope.application", resource.getApplication());
        if(resource.getLifeCycleStatus()!=null){
            fields.put("lifeCycleStatus",resource.getLifeCycleStatus().name()); 
        }
        if(resource.getAccessAdGroup()!=null){
            fields.put("accessAdGroup",resource.getAccessAdGroup()); 
        }
        for (Entry<String, String> entry : fields.entrySet()) {
            if (entry.getValue() != null) {
                data.addFormData(entry.getKey(), entry.getValue(), MediaType.TEXT_PLAIN_TYPE);
            }
        }
        for (PropertyElement element : resource.getProperties()) {
            data.addFormData(element.getName(), element.getValue(), MediaType.TEXT_PLAIN_TYPE);
        }
        return data;
    }

    public Set<ResourceElement> findUsedResourcesFromCache() {
        Set<ResourceElement> usedResources = new HashSet<>();
        for (Object cacheObject : cache.values()) {
            if (cacheObject instanceof ResourceElement) {
                usedResources.add((ResourceElement) cacheObject);
            }
        }

        return usedResources;
    }

    public Response deleteNode(String hostname, String comment) {
        URI url = getBaseUrl().path("nodes").path(hostname).build();
        cache.clear();
        return delete(url, comment);
    }

    private UriBuilder withComment(UriBuilder uriBuilder, String comment) {
        if (comment != null) {
            return uriBuilder.queryParam("entityStoreComment", comment);
        }
        return uriBuilder;
    }

    private Response delete(URI url, String comment) {
        try {
            String urlString = withComment(UriBuilder.fromUri(url), comment).build().toString();
            ClientResponse<?> response = createClientRequest(urlString).delete();
            checkResponse(response, url);
            response.releaseConnection();
            log.debug("DELETE {} with comment {}", url, comment);
            return response;
        } catch (Exception e) {
            log.warn("Could not DELETE {} with comment {}", url, comment);
            throw rethrow(e);
        }
    }

    private Response post(URI url, Object data, String comment) {
        return post(url, data, comment, MediaType.APPLICATION_XML);
    }

    private Response post(URI url, Object data, String comment, String mediaType) {
        try {
            String urlString = withComment(UriBuilder.fromUri(url), comment).build().toString();
            ClientRequest request = createClientRequest(urlString).body(mediaType, data);
            ClientResponse<?> response = request.post();
            checkResponse(response, url);
            response.releaseConnection();
            log.debug("POST {} with comment {}", url, comment);
            return response;
        } catch (Exception e) {
            log.warn("Could not POST {} with comment {}", url, comment);
            throw rethrow(e);
        }
    }

    private Response put(URI url, Object data, String comment) {
        try {
            String urlString = withComment(UriBuilder.fromUri(url), comment).build().toString();
            ClientRequest request = createClientRequest(urlString).body(MediaType.APPLICATION_XML, data);
            ClientResponse<?> response = request.put();
            checkResponse(response, url);
            response.releaseConnection();
            log.debug("PUT {} with comment {}", url, comment);
            return response;
        } catch (Exception e) {
            log.warn("Could not PUT {} with comment {}", url, comment);
            throw rethrow(e);
        }
    }

    private ClientRequest createClientRequest(String url) {
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        ApacheHttpClient4Executor clientExecutor = new ApacheHttpClient4Executor(httpClient, httpContext);

        ClientRequest clientRequest = new ClientRequest(url, clientExecutor);
        if (onBehalfOf != null) {
            clientRequest.header("x-onbehalfof", onBehalfOf);
        }
        return clientRequest;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(URI url, Class<T> returnType) {
        try {
            if (cache.containsKey(url)) {
                log.debug("Fetching {} from cache", url);
                return (T) cache.get(url);
            }
            log.debug("Calling url {}", url);
            ClientRequest client = createClientRequest(url.toString());
            ClientResponse<T> response = client.get(returnType);
            checkResponse(response, url);
            T result = response.getEntity();
            putInCache(url, result);
            return result;
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    private <T> void putInCache(URI url, T result) {
        if (result instanceof InputStream) {
            log.debug("No caching of streams");
            return;
        }
        if (useCache) {
            cache.put(url, result);
        }
    }

    private <T> void checkResponse(ClientResponse<T> response, URI requestUrl) {
        Status status = response.getResponseStatus();
        if (status == Status.FORBIDDEN) {
            response.releaseConnection();
            throw new SecurityException("Access forbidden to " + requestUrl);
        }
        if (status == Status.UNAUTHORIZED) {
            response.releaseConnection();
            throw new SecurityException("Unautorized access to " + requestUrl);
        }
        if (status == Status.NOT_FOUND) {
            response.releaseConnection();
            throw new IllegalArgumentException("Not found " + requestUrl);
        }
        if (status.getStatusCode() >= 400) {
            throw new RuntimeException("Error calling " + requestUrl + " code: " + status.getStatusCode() + "\n " + response.getEntity(String.class));
        }
    }

    /**
     * This is just a rethrow of resteasy client exceptions which should have been RuntimeExceptions. Who uses Exception as a
     * API exception in 2013?
     */
    private RuntimeException rethrow(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setOnBehalfOf(String onBehalfOf) {
        this.onBehalfOf = onBehalfOf;
    }

    public void useCache(boolean useCache) {
        this.useCache = useCache;
    }
}
