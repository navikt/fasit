package no.nav.aura.envconfig.client;

import no.nav.aura.envconfig.client.rest.PropertyElement;
import no.nav.aura.envconfig.client.rest.ResourceElement;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

public class FasitRestClientTest {

    private RestTemplate restTemplateMock;
    private FasitRestClient client;
    private ResponseEntity responseMock;

    
    @BeforeEach
    public void setUp() {
        restTemplateMock = Mockito.mock(RestTemplate.class);
        client = new FasitRestClient("http://someserver.com", "user", "password");
        client.setRestTemplate(restTemplateMock);
        responseMock = Mockito.mock(ResponseEntity.class);
        when(responseMock.getStatusCode()).thenReturn(HttpStatus.OK);
        when(responseMock.getStatusCodeValue()).thenReturn(200);
        when(responseMock.getHeaders()).thenReturn(org.springframework.http.HttpHeaders.EMPTY);
        when(responseMock.hasBody()).thenReturn(true);
    }
    
    @Test
    public void testGetApplicationInfo() throws Exception {
        ApplicationDO applicationDO = new ApplicationDO();
        applicationDO.setName("myapp");
        applicationDO.setAppConfigArtifactId("myapp");
        applicationDO.setAppConfigGroupId("no.nav.aura.test");
        when(responseMock.getBody()).thenReturn(applicationDO);

        when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(ApplicationDO.class))).thenReturn(responseMock);
        ApplicationDO result = client.getApplicationInfo("myapp");
        assertEquals(applicationDO, result);
    }

    @Test
    public void testGetNodeCount() throws Exception {
    	
        ApplicationInstanceDO applicationInstanceDO = new ApplicationInstanceDO("bpm", "t5", UriComponentsBuilder.fromHttpUrl("http://someserver.com"));
												
        ClusterDO clusterDO = new ClusterDO();
        clusterDO.setEnvironmentClass("t");
        clusterDO.setEnvironmentName("t5");
        clusterDO.setName("bpmCluster");
        clusterDO.addNode(new NodeDO("d26wasl00002.test.local", PlatformTypeDO.WAS));
        clusterDO.addNode(new NodeDO("d26wasl00003.test.local", PlatformTypeDO.WAS));
        applicationInstanceDO.setCluster(clusterDO);
        
        when(responseMock.getBody()).thenReturn(applicationInstanceDO);
        when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(ApplicationInstanceDO.class)))
            .thenReturn(responseMock);
        
        assertEquals(2, client.getNodeCount("t5", "bpm"));
    }

    @Test
    public void testGetApplicatInstance() throws Exception {
        ApplicationInstanceDO applicationInstanceDO = new ApplicationInstanceDO("myapp", "u1", UriComponentsBuilder.fromHttpUrl("http://someserver.com"));
        ClusterDO clusterDO = new ClusterDO();
        clusterDO.setEnvironmentClass("u");
        clusterDO.setEnvironmentName("u1");
        clusterDO.setName("myappCluster");
        clusterDO.addNode(new NodeDO("myapp.node1.com", PlatformTypeDO.WAS));
        clusterDO.addNode(new NodeDO("myapp.node2.com", PlatformTypeDO.WAS));
        applicationInstanceDO.setCluster(clusterDO);
        
        when(responseMock.getBody()).thenReturn(applicationInstanceDO);
        when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(ApplicationInstanceDO.class)))
            .thenReturn(responseMock);
                
        ApplicationInstanceDO response =  client.getApplicationInstance("u1", "myapp");
        assertEquals(applicationInstanceDO, response);
        assertEquals("myappCluster", response.getCluster().getName());
    }

    @Test
    public void testGetResourceProperties() throws Exception {
        ResourceElement resourceElement = new ResourceElement(ResourceTypeDO.BaseUrl, "myUrl");
        resourceElement.setId(123L);
        resourceElement.setEnvironmentClass("u");
        resourceElement.addProperty(new PropertyElement("url", "http://myUrl"));

        when(responseMock.getBody()).thenReturn(resourceElement);
        when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(ResourceElement.class)))
            .thenReturn(responseMock);
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
        when(responseMock.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(responseMock.getStatusCodeValue()).thenReturn(404);
        when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(ResourceElement.class)))
            .thenReturn(responseMock);
        try {
            ResourceElement resource = client.getResourceById(123);
        } catch (Exception e) {
            e.printStackTrace();
        }
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            client.getResourceById(123);
        });
        
        // Optionally verify the exception message
        assertEquals("Not found http://someserver.com/resources/123", exception.getMessage());
    }

    @Test
    public void testFile() throws Exception {
    	String expectedContent = "myfile.txt";
        byte[] fileContent = expectedContent.getBytes();
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(fileContent, HttpStatus.OK);

        
		when(responseMock.getStatusCode()).thenReturn(HttpStatus.OK);
		when(responseMock.getStatusCodeValue()).thenReturn(200);
		when(responseMock.hasBody()).thenReturn(true);
		when(responseMock.getBody()).thenReturn(mockResponse);
        when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), 
                eq(byte[].class)))
                .thenReturn(mockResponse);
        
        InputStream result= client.getFile(URI.create("http://someserver.com"));
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(result, stringWriter);
        assertEquals(expectedContent, stringWriter.toString());
        verify(restTemplateMock).exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(byte[].class));
    }

    @Test
    public void testUnautorized() throws Exception {
        Assertions.assertThrows(SecurityException.class, () -> {
            when(responseMock.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
            when(responseMock.getStatusCodeValue()).thenReturn(401);
            when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(responseMock);
            client.getSecret(URI.create("http://someserver.com"));
        });
    }

    @Test
    public void testForbidden() throws Exception {
        Assertions.assertThrows(SecurityException.class, () -> {
            when(responseMock.getStatusCode()).thenReturn(HttpStatus.FORBIDDEN);
            when(responseMock.getStatusCodeValue()).thenReturn(403);
            when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(responseMock);
            client.getSecret(URI.create("http://someserver.com"));
        });
    }

    @Test
    public void testNotFound() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            when(responseMock.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
            when(responseMock.getStatusCodeValue()).thenReturn(404);
            when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(responseMock);
            client.getSecret(URI.create("http://someserver.com"));
        });
    }

}
