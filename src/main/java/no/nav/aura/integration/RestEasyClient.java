package no.nav.aura.integration;

import java.net.URI;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestEasyClient {

    private static final Logger log = LoggerFactory.getLogger(RestEasyClient.class);

    private Client client;

    public RestEasyClient(String username, String password) {
        client = ClientBuilder.newClient();
        client.register(new BasicAuthentication(username, password));
    }

    WebTarget createRequest(URI url) {
        return client.target(url);
    }

    <T> T get(URI url, Class<T> returnType) {
        Response response = createRequest(url).request().get();
        log.debug("Calling url {}", url);
        // ClientRequest client = createClientRequest(url);
        // ClientResponse<T> response = client.get(returnType);
        T result = response.readEntity(returnType);
        checkResponseAndThrowExeption(response, url);
        response.close();
        return result;
    }

    void checkResponseAndThrowExeption(Response response, URI requestUrl) {
        int status = response.getStatus();
        if (status == 403) {
            response.close();
            throw new ForbiddenException("Access forbidden to " + requestUrl);
        }
        if (status == 401) {
            response.close();
            throw new NotAuthorizedException("Unautorized access to " + requestUrl);
        }

        if (status == 404) {
            response.close();
            throw new NotFoundException("Not found " + requestUrl);
        }

        if (status >= 400) {
            String entity = null;
            try {
                entity = response.readEntity(String.class);
            } catch (Exception e) {
                log.error("Unable to get fault reason", e);
            }
            response.close();
            throw new WebApplicationException("Error calling " + requestUrl + entity, status);
        }
    }
}
