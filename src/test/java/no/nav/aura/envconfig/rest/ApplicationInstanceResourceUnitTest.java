package no.nav.aura.envconfig.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import no.nav.aura.sensu.SensuClient;
import org.hamcrest.Matchers;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.junit.jupiter.api.*;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
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
        service = new ApplicationInstanceResource(repository, instanceRepository, mock(SensuClient.class), mock(FasitKafkaProducer.class));
    }

    @Test
    public void invalidJsonYieldsBadRequest() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            ApplicationInstanceResource.validateJson("<xml />");
        });
    }

    @Test
    public void missingRequiredPropertiesYieldsBadRequest() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            ApplicationInstanceResource.schemaValidateJsonString("/registerApplicationInstanceSchema.json", "{}");
        });
    }

    @Test
    public void acceptsValidPayload() {
        String validPayload = no.nav.aura.envconfig.rest.ClasspathResourceHelper.getStringFromFileOnClassPath("/payloads/registerapplicationinstance-max.json");
        assertEquals(ApplicationInstanceResource.schemaValidateJsonString("/registerApplicationInstanceSchema.json", validPayload), validPayload);
    }

    @Test
    public void nonexistentApplicationYields404() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            service.verifyApplicationExists("-_-");
        });
    }

    @Test
    public void nonexistentEnvironmentYields404() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            service.verifyEnvironmentExists("^_^");
        });
    }

    @Test
    public void nonexistentNodesYields404() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            service.verifyNodesExist(Lists.newArrayList("bleep", "blaap", "bloop"));
        });
    }

    @Test
    public void nonexistentApplicationMappingInEnvironmentYields404() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            service.verifyApplicationIsDefinedInEnvironment("bop", "shoo");
        });
    }

    @Test
    public void nonexistentUsedResourcesYieldsBadRequest() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            when(repository.getRevision(Resource.class, 1337, 69)).thenThrow(RevisionDoesNotExistException.class);
            UsedResource nonexistentUsedResource = new UsedResource(1337, 69);
            service.verifyUsedResources(Lists.newArrayList(nonexistentUsedResource));
        });
    }

    @Test
    public void validMissingResourcePassesVerification() {
        MissingResource missingResourceWithInvalidType = new MissingResource("thetvshow", ResourceTypeDO.BaseUrl);
        ApplicationInstanceResource.verifyMissingResources(Lists.newArrayList(missingResourceWithInvalidType));
    }

    @Test
    public void exposedResourcesLackingMandatoryFieldsYieldsBadRequest() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            ExposedResource exposedResourceLackingMandatoryFields = new ExposedResource(ResourceTypeDO.BaseUrl.name(), null, null);
            service.verifyExposedResources(Lists.newArrayList(exposedResourceLackingMandatoryFields));
        });
    }

    @Test
    public void exposedResourcesWithNonexistentFieldsYieldsBadRequest() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            ExposedResource exposedResourceWithNonexistentFields = new ExposedResource(ResourceTypeDO.BaseUrl.name(), "alias", ImmutableMap.of("url", "http://baseurl.com", "blapp", "setter"));
            service.verifyExposedResources(Lists.newArrayList(exposedResourceWithNonexistentFields));
        });
    }

    @Test
    public void exposedResourcesWithNonexistentResourceTypeYieldsBadRequest() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            ExposedResource exposedResourceWithNonexistentResourceType = new ExposedResource("nonexistentResourceType", null, null);
            service.verifyExposedResources(Lists.newArrayList(exposedResourceWithNonexistentResourceType));
        });
    }

    @Test
    public void validExposedResourcePassesVerification() {
        ExposedResource validExposedResource = new ExposedResource(ResourceTypeDO.BaseUrl.name(), "mybaseurl", ImmutableMap.of("url", "http://baseurl.com"));
        service.verifyExposedResources(Lists.newArrayList(validExposedResource));
    }

    @Test
    public void validExposedQueuePassesVerification() {
        ExposedResource exposedQueue = new ExposedResource(ResourceTypeDO.Queue.name(), "myQueue", 123L, null);
        when(repository.getById(Resource.class, 123L)).thenReturn(new Resource("myQueue", ResourceType.Queue, new Scope(EnvironmentClass.u)));
        service.verifyExposedResources(Lists.newArrayList(exposedQueue));
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
            service.verifyExposedResources(Lists.newArrayList(exposedQueue));
            Assertions.fail("No exception");
        } catch (BadRequestException e) {
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

        Set<ExposedServiceReference> resources = Sets.newHashSet(
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
