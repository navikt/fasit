package no.nav.aura.envconfig.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.util.UriComponentsBuilder;

import no.nav.aura.appconfig.Application;
import no.nav.aura.envconfig.client.DomainDO.EnvClass;
import no.nav.aura.envconfig.client.rest.PropertyElement;
import no.nav.aura.envconfig.client.rest.ResourceElement;
import no.nav.aura.envconfig.client.rest.ResourceElementList;
import no.nav.aura.fasit.client.model.RegisterApplicationInstancePayload;

public class FasitRestClient {

    private URI baseUrl;
    private RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(FasitRestClient.class);
    private Map<URI, Object> cache = new HashMap<URI, Object>();
    private String onBehalfOf;
    private boolean useCache = true;

    public FasitRestClient(String baseUrl, String username, String password) {
        this.baseUrl = URI.create(baseUrl);
        this.restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new FasitResponseErrorHandler());
        restTemplate.getInterceptors().add((request, body, execution) -> {
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            request.getHeaders().set("Authorization", authHeader);
            
            if (onBehalfOf != null) {
                request.getHeaders().set("x-onbehalfof", onBehalfOf);
            }
            
            return execution.execute(request, body);
        });
        
        log.info("using rest based envconfig client with url : {} and user {}", baseUrl, username);
    }
  
    public UriComponentsBuilder getBaseUrl() {
        return UriComponentsBuilder.fromUri(baseUrl);
    }

    public ApplicationInstanceDO getApplicationInstance(String environment, String appName) {
        URI url = getBaseUrl().path("/environments/{env}/applications/{app}").buildAndExpand(environment, appName).toUri();
        ApplicationInstanceDO appInstance = get(url, ApplicationInstanceDO.class);
        return appInstance;
    }

    public Collection<EnvironmentDO> getEnvironments() {
        URI url = getBaseUrl().path("/environments").build().toUri();
        EnvironmentDO[] environments = get(url, EnvironmentDO[].class);
        return Arrays.asList(environments);
    }

    public ApplicationInstanceListDO getApplicationInstances(String environment) {
        URI url = getBaseUrl().path("/environments/{env}/applications").build(environment);
        ApplicationInstanceListDO instances = get(url, ApplicationInstanceListDO.class);
        return instances;
    }

    public String getSecret(URI url) {
        try {
            if (cache.containsKey(url)) {
                log.debug("Fetching {} from cache", url);
                return (String) cache.get(url);
            }
            log.debug("Calling url {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.add("showsecret", "true");
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            
            checkResponse(response, url);
            String result = response.getBody();

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
    public ResourceElementList findResources(EnvClass envClass, String environment, DomainDO domain, String appName, ResourceTypeDO type, String alias) {
        URI url = buildResourceQuery(envClass, environment, domain, appName, type, alias, false, false);
        log.info("Finding resources with url {}", url);
        ResourceElementList resources = get(url, ResourceElementList.class);
        return resources;
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
        UriComponentsBuilder uriBuilder = getBaseUrl().path("/resources");

        if (envClass != null) {
            uriBuilder.queryParam("envClass", envClass);
        }
        if (environment != null) {
        	uriBuilder.queryParam("envName", environment);
        }
        if (domain != null) {
        	uriBuilder.queryParam("domain", domain.getFqn());
        }
        if (appName != null) {
        	uriBuilder.queryParam("app", appName);
        }
        if (type != null) {
        	uriBuilder.queryParam("type", type);
        }
        if (alias != null) {
        	uriBuilder.queryParam("alias", alias);
        }
        if(bestMatch !=null){
        	uriBuilder.queryParam("bestmatch", bestMatch);
        }
        if(usage !=null){
        	uriBuilder.queryParam("usage", usage);
        }

        URI url = uriBuilder.build().toUri();
        log.debug("REST URL " + url);
        return url;
    }

    public boolean resourceExists(EnvClass envClass, String environment, DomainDO domain, String appName, ResourceTypeDO type, String alias) {
        return !findResources(envClass, environment, domain, appName, type, alias).isEmpty();
    }

    /** Find the best matching resource given a full scope */
    public ResourceElement getResource(String environment, String alias, ResourceTypeDO type, DomainDO domain, String appName) {
        URI url = getBaseUrl().path("/resources/bestmatch")
                .queryParam("envName", environment)
                .queryParam("domain", domain.getFqn())
                .queryParam("type", type)
                .queryParam("alias", alias)
                .queryParam("app", appName)
                .build().toUri();
        log.debug("REST URL " + url);
        ResourceElement resource = get(url, ResourceElement.class);
        return resource;
    }

    public ResourceElement getResourceById(long resourceId) {
        URI url = getBaseUrl().path("/resources/{id}")
                .buildAndExpand(resourceId).toUri();
        log.debug("REST URL " + url);
        ResourceElement resource = get(url, ResourceElement.class);
        return resource;
    }

    public ApplicationDO getApplicationInfo(String appName) {
        URI url = getBaseUrl().path("/applications/{appname}").buildAndExpand(appName).toUri();
        return get(url, ApplicationDO.class);
    }

    public ApplicationGroupDO getApplicationGroup(String name) {
        URI url = getBaseUrl().path("/applicationGroups/{name}").buildAndExpand(name).toUri();
        return get(url, ApplicationGroupDO.class);
    }

    public ResponseEntity<String> registerApplication(RegisterApplicationInstancePayload payload, String comment) {
        URI url = getBaseUrl().path("/v1/applicationinstances").build().toUri();
        log.debug("Registering new application instance to endpoint {} with payload {}", url, payload.toJson());
        return post(url, payload.toJson(), comment, MediaType.APPLICATION_JSON);
    }


    public ResponseEntity<String> undeployApplication(String environmentName, String applicationName, String comment) {
        URI url = getBaseUrl().path("/environments/{env}/applications/{app}").buildAndExpand(environmentName, applicationName).toUri();
        log.debug("Undeploying application {} on {} ", applicationName, url);
        return delete(url, comment);
    }

    public ResponseEntity<String> verifyApplication(String environment, String applicationName, Application application) {
        URI url = getBaseUrl().path("/environments/{env}/applications/{app}/verify").buildAndExpand(environment, applicationName).toUri();
        log.debug("Verify application {} on {} ", applicationName, url);
        return put(url, application, null);
    }

    public NodeDO registerNode(NodeDO nodeDO, String comment) {
        URI uri = withComment(getBaseUrl().path("/nodes"), comment).build().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        try {
    		ResponseEntity<NodeDO> response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(nodeDO, headers), NodeDO.class);
            checkResponse(response, uri);
            return response.getBody();
        	
        } catch (UnknownContentTypeException ex) {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(nodeDO, headers), String.class);
            checkResponse(response, uri);
        	log.error("Unknown content type when registering node: {}", response.getBody());
            throw new IllegalArgumentException("Resource not found at " + uri + ": " + response.getBody());
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
    public ResponseEntity<String> updateNode(NodeDO nodeDO, String comment) {
        URI uri = getBaseUrl().path("/nodes/{hostname}")
                .buildAndExpand(nodeDO.getHostname()).toUri();
        log.debug("Updating node on url", uri);
        return post(uri, nodeDO, comment);
    }

    public ResponseEntity<String> deleteResource(long id, String comment) {
        URI url = getBaseUrl().path("/resources/{id}")
                .buildAndExpand(id).toUri();
        return delete(url, comment);
    }

    public ResourceElement updateResource(long id, ResourceElement resource, String comment) {
    	MultiValueMap<String, Object> data = createFormData(resource);
        return executeMultipart(HttpMethod.POST, "/resources/" + id, data, comment, ResourceElement.class);
    }

    /**
     * NB: Not implemented for file resources
     * 
     * @return
     */
    public ResourceElement registerResource(ResourceElement resource, String comment) {
    	MultiValueMap<String, Object> data = createFormData(resource);
        return executeMultipart(HttpMethod.PUT, "/resources", data, comment, ResourceElement.class);
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

    public <T> T executeMultipart(HttpMethod method, String path, MultiValueMap<String, Object> data, String comment, Class<T> responseClass) {
        URI url = withComment(getBaseUrl().path(path), comment).build().toUri();
        try {
            log.debug("Sending multipart to {} with method {} ", url, method);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(data, headers);
            
            ResponseEntity<T> response = restTemplate.exchange(
                url, method, requestEntity, responseClass);
            
            checkResponse(response, url);
            return response.getBody();
        } catch (Exception e) {
            log.warn("unable to register resource", e);
            throw rethrow(e);
        }
    }

    private MultiValueMap<String, Object> createFormData(ResourceElement resource) {
        MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
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
                data.add(entry.getKey(), entry.getValue());
            }
        }
        for (PropertyElement element : resource.getProperties()) {
            data.add(element.getName(), element.getValue());
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

    public ResponseEntity<String> deleteNode(String hostname, String comment) {
        URI url = getBaseUrl().path("/nodes/{node}").buildAndExpand(hostname).toUri();
        cache.clear();
        return delete(url, comment);
    }

    private UriComponentsBuilder withComment(UriComponentsBuilder uriBuilder, String comment) {
        if (comment != null) {
            return uriBuilder.queryParam("entityStoreComment", comment);
        }
        return uriBuilder;
    }

    private ResponseEntity<String> delete(URI url, String comment) {
        try {
            URI uri = withComment(UriComponentsBuilder.fromUri(url), comment).build().toUri();
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.DELETE, null, String.class);
            
            checkResponse(response, url);
            log.debug("DELETE {} with comment {}", url, comment);
            return response;
        } catch (Exception e) {
            log.warn("Could not DELETE {} with comment {}", url, comment);
            throw rethrow(e);
        }
    }

    private ResponseEntity<String> post(URI url, Object data, String comment) {
        return post(url, data, comment, MediaType.APPLICATION_XML);
    }

    private ResponseEntity<String> post(URI url, Object data, String comment, MediaType mediaType) {
        try {
            URI uri = withComment(UriComponentsBuilder.fromUri(url), comment).build().toUri();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            
            HttpEntity<?> requestEntity = new HttpEntity<>(data, headers);
            log.debug("POST {} with comment {}", url, comment);
            ResponseEntity<String> response = restTemplate.exchange(
                uri, HttpMethod.POST, requestEntity, String.class);
            
            checkResponse(response, url);
            return response;
        } catch (Exception e) {
            log.warn("Could not POST {} with comment {}", url, comment);
            throw rethrow(e);
        }
    }

    private ResponseEntity<String> put(URI url, Object data, String comment) {
        try {
            URI uri = withComment(UriComponentsBuilder.fromUri(url), comment).build().toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            
            HttpEntity<?> requestEntity = new HttpEntity<>(data, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                uri, HttpMethod.PUT, requestEntity, String.class);
            
            checkResponse(response, url);
            log.debug("PUT {} with comment {}", url, comment);
            return response;
        } catch (Exception e) {
            log.warn("Could not PUT {} with comment {}", url, comment);
            throw rethrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(URI url, Class<T> returnType) {
        try {
            if (cache.containsKey(url)) {
                log.debug("Fetching {} from cache", url);
                return (T) cache.get(url);
            }
            log.debug("Calling url {}", url);
            ResponseEntity<?> response;
            if (InputStream.class.isAssignableFrom(returnType)) {
                response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
                checkResponse(response, url);
                byte[] responseBody = (byte[]) response.getBody();
                return (T) new ByteArrayInputStream(responseBody);
            } else {
            	try {
	                response = restTemplate.exchange(url, HttpMethod.GET, null, returnType);
	                checkResponse(response, url);
	                T result = (T) response.getBody();
	                putInCache(url, result);
	                return result;
            	} catch (UnknownContentTypeException ex) {
                    response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
                    checkResponse(response, url);
                    throw new IllegalArgumentException("Resource not found at " + url + ": " + response.getBody());
                }	
            }
        } catch (Exception e) {
            log.error("Error while calling {}", url, e);
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

    private <T> void checkResponse(ResponseEntity<?> response, URI requestUrl) {
        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.FORBIDDEN) {
            throw new SecurityException("Access forbidden to " + requestUrl);
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            throw new SecurityException("Unauthorized access to " + requestUrl);
        }
        if (status == HttpStatus.NOT_FOUND) {
            throw new IllegalArgumentException("Not found " + requestUrl);
        }
        if (status.value() >= 400) {
            throw new RuntimeException("Error calling " + requestUrl + " code: " + status.value() + 
                                       "\n " + response.getBody());
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

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public void setOnBehalfOf(String onBehalfOf) {
        this.onBehalfOf = onBehalfOf;
    }

    public void useCache(boolean useCache) {
        this.useCache = useCache;
    }
    
    private static class FasitResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            // Let the checkResponse method handle the error
        }
    }
}
