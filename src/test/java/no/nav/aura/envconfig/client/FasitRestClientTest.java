package no.nav.aura.envconfig.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import no.nav.aura.envconfig.client.rest.PropertyElement;
import no.nav.aura.envconfig.client.rest.ResourceElement;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FasitRestClientTest {

    private HttpClient httpMock;
    private FasitRestClient client;

    @BeforeEach
    public void setUp() {
        httpMock = Mockito.mock(HttpClient.class);
        client = new FasitRestClient("http://someserver.com", "user", "password");
        client.setHttpClient(httpMock);
    }

    @Test
    public void testGetApplicationInfo() throws Exception {
        HttpResponse response = prepareResponse(200,
                "<application><appConfigArtifactId>myapp</appConfigArtifactId>" +
                        "<appConfigGroupId>no.nav.aura.test</appConfigGroupId>" +
                        "<name>myapp</name></application>");
        when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
        client.getApplicationInfo("myapp");
    }

    @Test
    public void testGetNodeCount() throws Exception {
        HttpResponse response = prepareResponse(200,
                "<application>" +
                        "<cluster>" +
                        "<baseUrl>https://d26wasl00002.test.local</baseUrl>" +
                        "<domain>test.local</domain>" +
                        "<environmentClass>t</environmentClass>" +
                        "<environmentName>t5</environmentName>" +
                        "<name>bpm</name>" +
                        "<nodes>" +
                        "<hostname>d26wasl00002.test.local</hostname>" +
                        "<username>deployer</username><domain>test.local</domain>" +
                        "<passwordRef>http://e34apsl00156.devillo.no:8080/conf/secrets/secret-6</passwordRef>" +
                        "</nodes><type>WAS</type>" +
                        "<nodes>" +
                        "<hostname>d26wasl00003.test.local</hostname>" +
                        "<username>deployer</username><domain>test.local</domain>" +
                        "<passwordRef>http://e34apsl00156.devillo.no:8080/conf/secrets/secret-6</passwordRef>" +
                        "</nodes><type>WAS</type></cluster></application>");
        when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
        assertEquals(2, client.getNodeCount("t5", "bpm"));
    }

    @Test
    public void testGetApplicatInstance() throws Exception {
        HttpResponse response = prepareResponse(200,
                "<application>" +
                        "<cluster>" +
                        "<baseUrl>https://e34jbsl00390.devillo.no:8443</baseUrl>" +
                        "<domain>devillo.no</domain>" +
                        "<environmentClass>u</environmentClass>" +
                        "<environmentName>tpr-u1</environmentName>" +
                        "<name>autodeployCluster</name>" +
                        "<nodes>" +
                        "<hostname>e34jbsl00390.devillo.no</hostname>" +
                        "<username>deployer</username><domain>devillo.no</domain>" +
                        "<passwordRef>http://e34apsl00156.devillo.no:8080/conf/secrets/secret-6</passwordRef>" +
                        "</nodes><type>jboss</type></cluster></application>");
        when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
        client.getApplicationInstance("u1", "myapp");
    }

    @Test
    public void testGetResourceProperties() throws Exception {
        HttpResponse response = prepareResponse(200,
                "<resource>" +
                        "<type>BaseUrl</type>" +
                        "<id>123</id>" +
                        "<alias>myUrl</alias>" +
                        "<environmentClass>u</environmentClass>" +
                        "<property name=\"url\" type=\"STRING\">" +
                        "<value>http://myUrl</value>" +
                        "</property>" +
                        "</resource>");
        when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
        ResourceElement resource = client.getResource("u1", "alias", ResourceTypeDO.DataSource, DomainDO.Devillo, "myapp");
        assertEquals("myUrl", resource.getAlias());
        assertEquals(Long.valueOf(123), resource.getId());
        assertEquals(ResourceTypeDO.BaseUrl, resource.getType());
        assertEquals("u", resource.getEnvironmentClass());
        assertEquals(1, resource.getProperties().size());
        PropertyElement element = resource.getProperties().iterator().next();
        assertEquals(PropertyElement.Type.STRING, element.getType());
        assertEquals("url", element.getName());
        assertEquals("http://myUrl", element.getValue());
    }

    @Test
    public void test() throws Exception {
        HttpResponse response = prepareResponse(404,
                "No resource with id 123 found");
        when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
        try {
            ResourceElement resource = client.getResourceById(123);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFile() throws Exception {
        HttpResponse response = prepareResponse(200, "myfile.txt");
        when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
        client.getFile(URI.create("http://someserver.com"));
    }

    @Test
    public void testUnautorized() throws Exception {
        Assertions.assertThrows(SecurityException.class, () -> {
            HttpResponse response = prepareResponse(401, "");
            when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
            client.getSecret(URI.create("http://someserver.com"));
        });
    }

    @Test
    public void testForbidden() throws Exception {
        Assertions.assertThrows(SecurityException.class, () -> {
            HttpResponse response = prepareResponse(403, "");
            when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
            client.getSecret(URI.create("http://someserver.com"));
        });
    }

    @Test
    public void testNotFound() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            HttpResponse response = prepareResponse(404, "");
            when(httpMock.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
            client.getSecret(URI.create("http://someserver.com"));
        });
    }

    private HttpResponse prepareResponse(int expectedResponseStatus, String expectedResponseBody) {
        HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), expectedResponseStatus, null);
        try {
            response.setEntity(new StringEntity(expectedResponseBody));
            response.setHeader("Content-type", "application/xml");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        return response;
    }
}
