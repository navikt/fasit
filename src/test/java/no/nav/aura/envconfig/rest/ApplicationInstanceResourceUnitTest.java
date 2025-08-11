package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.JPAFasitRepository;
import no.nav.aura.envconfig.client.ResourceTypeDO;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.ExposedServiceReference;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.fasit.client.model.ExposedResource;
import no.nav.aura.fasit.client.model.MissingResource;
import no.nav.aura.fasit.client.model.UsedResource;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.integration.FasitKafkaProducer;
import org.hamcrest.Matchers;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApplicationInstanceResourceUnitTest {

    private FasitRepository repository;
    private ApplicationInstanceResource service;
    private ApplicationInstanceRepository instanceRepository;

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");

    }

    @AfterAll
    public static void afterClass() {
        System.clearProperty("fasit.encryptionkeys.username");
        System.clearProperty("fasit.encryptionkeys.password");
    }

    @BeforeEach
    public void setUp() {
        repository = mock(JPAFasitRepository.class);
        instanceRepository = mock(ApplicationInstanceRepository.class);
        service = new ApplicationInstanceResource(repository, instanceRepository, mock(FasitKafkaProducer.class));
    }

    @Test
    public void invalidJsonYieldsBadRequest() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            ApplicationInstanceResource.validateJson("<xml />");
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void missingRequiredPropertiesYieldsBadRequest() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            ApplicationInstanceResource.schemaValidateJsonString("/registerApplicationInstanceSchema.json", "{}");
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void acceptsValidPayload() {
        String validPayload = no.nav.aura.envconfig.rest.ClasspathResourceHelper.getStringFromFileOnClassPath("/payloads/registerapplicationinstance-max.json");
        assertEquals(ApplicationInstanceResource.schemaValidateJsonString("/registerApplicationInstanceSchema.json", validPayload), validPayload);
    }

    @Test
    public void nonexistentApplicationYields404() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            service.verifyApplicationExists("-_-");
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void nonexistentEnvironmentYields404() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            service.verifyEnvironmentExists("^_^");
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void nonexistentNodesYields404() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            service.verifyNodesExist(Arrays.asList("bleep", "blaap", "bloop"));
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void nonexistentApplicationMappingInEnvironmentYields404() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            service.verifyApplicationIsDefinedInEnvironment("bop", "shoo");
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void nonexistentUsedResourcesYieldsNotFound() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            when(repository.getRevision(Resource.class, 1337, 69)).thenThrow(RevisionDoesNotExistException.class);
            UsedResource nonexistentUsedResource = new UsedResource(1337, 69);
            service.verifyUsedResources(Arrays.asList(nonexistentUsedResource));
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void validMissingResourcePassesVerification() {
        MissingResource missingResourceWithInvalidType = new MissingResource("thetvshow", ResourceTypeDO.BaseUrl);
        ApplicationInstanceResource.verifyMissingResources(Arrays.asList(missingResourceWithInvalidType));
    }

    @Test
    public void exposedResourcesLackingMandatoryFieldsYieldsBadRequest() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            ExposedResource exposedResourceLackingMandatoryFields = new ExposedResource(ResourceTypeDO.BaseUrl.name(), null, null);
            service.verifyExposedResources(Arrays.asList(exposedResourceLackingMandatoryFields));
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void exposedResourcesWithNonexistentFieldsYieldsBadRequest() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            ExposedResource exposedResourceWithNonexistentFields = new ExposedResource(ResourceTypeDO.BaseUrl.name(), "alias", Map.of("url", "http://baseurl.com", "blapp", "setter"));
            service.verifyExposedResources(Arrays.asList(exposedResourceWithNonexistentFields));
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void exposedResourcesWithNonexistentResourceTypeYieldsBadRequest() {
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            ExposedResource exposedResourceWithNonexistentResourceType = new ExposedResource("nonexistentResourceType", null, null);
            service.verifyExposedResources(Arrays.asList(exposedResourceWithNonexistentResourceType));
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void validExposedResourcePassesVerification() {
        ExposedResource validExposedResource = new ExposedResource(ResourceTypeDO.BaseUrl.name(), "mybaseurl", Map.of("url", "http://baseurl.com"));
        service.verifyExposedResources(Arrays.asList(validExposedResource));
    }

    @Test
    public void validExposedQueuePassesVerification() {
        ExposedResource exposedQueue = new ExposedResource(ResourceTypeDO.Queue.name(), "myQueue", 123L, null);
        when(repository.getById(Resource.class, 123L)).thenReturn(new Resource("myQueue", ResourceType.Queue, new Scope(EnvironmentClass.u)));
        service.verifyExposedResources(Arrays.asList(exposedQueue));
    }
    
    @Test
    public void missingIdExposedQueueYieldsBadRequest() {
        ExposedResource exposedQueue = new ExposedResource(ResourceTypeDO.Queue.name(), "myQueue", null, null);
        assertValidationException(exposedQueue, "required property id");
    }
    
    @Test
    public void notFoundExposedQueueYieldsBadRequest() {
        ExposedResource exposedQueue = new ExposedResource(ResourceTypeDO.Queue.name(), "myQueue", 123L, null);
        assertValidationException(exposedQueue, "not found in Fasit");
    }
    
    @Test
    public void otherTypeExposedQueueYieldsBadRequest() {
        ExposedResource exposedQueue = new ExposedResource(ResourceTypeDO.Queue.name(), "myQueue", 123L, null);
        when(repository.getById(Resource.class, 123L)).thenReturn(new Resource("myQueue", ResourceType.BaseUrl, new Scope(EnvironmentClass.u)));
        assertValidationException(exposedQueue, "other resourceType");
    }
    
    @Test
    public void otherAliasExposedQueueYieldsBadRequest() {
        ExposedResource exposedQueue = new ExposedResource(ResourceTypeDO.Queue.name(), "myQueue", 123L, null);
        when(repository.getById(Resource.class, 123L)).thenReturn(new Resource("otherQueue", ResourceType.Queue, new Scope(EnvironmentClass.u)));
        assertValidationException(exposedQueue, "other alias");
    }
    
    private void assertValidationException(ExposedResource exposedQueue, String expectedMessage) {
        try {
            service.verifyExposedResources(Arrays.asList(exposedQueue));
            Assertions.fail("No exception");
        } catch (ResponseStatusException e) {
            assertThat(e.getMessage(), Matchers.containsString(expectedMessage));
        }
    }
    

    @Test
    public void exposedResourcesWithoutDomainIsScopedToEnvironment() {
        ExposedResource exposedResource = new ExposedResource(ResourceTypeDO.BaseUrl.name(), "alias", null);
        Scope scope = ApplicationInstanceResource.createScopeForResource(exposedResource, new Environment("env", EnvironmentClass.p));
        assertNull(scope.getDomain());
    }

    @Test
    public void exposedResourcesWithDomainIsScopedToDomainAndEnvironment() {
        ExposedResource exposedResource = new ExposedResource(ResourceTypeDO.BaseUrl.name(), "alias", null);
        exposedResource.setDomain("adeo.no");
        Scope scope = ApplicationInstanceResource.createScopeForResource(exposedResource, new Environment("env", EnvironmentClass.p));
        assertNotNull(scope.getDomain());
    }

    @Test
    public void exposedServicesReference() {

        Set<ExposedServiceReference> resources = Set.of(
                new ExposedServiceReference(createResource("alias", ResourceType.OpenAm, new Scope(EnvironmentClass.p)), 1L),
                new ExposedServiceReference(createResource("alias2", ResourceType.Credential, new Scope(EnvironmentClass.p)), 1L),
                new ExposedServiceReference(createResource("alias3", ResourceType.Credential, new Scope(EnvironmentClass.p)), 1L));

        assertNotNull(service.getResourceFromReference(resources));

    }

    private Resource createResource(String alias, ResourceType resourceType, Scope scope) {
        Resource resource = new Resource(alias, resourceType, scope);
        return resource;
    }

}
