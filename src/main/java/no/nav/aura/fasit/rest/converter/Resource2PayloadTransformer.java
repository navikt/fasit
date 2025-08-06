package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Zone;
import no.nav.aura.envconfig.model.resource.FileEntity;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.Scope;
import no.nav.aura.envconfig.model.secrets.Secret;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.rest.ApplicationInstanceRest;
import no.nav.aura.fasit.rest.ResourceRest;
import no.nav.aura.fasit.rest.model.ResourcePayload;
import no.nav.aura.fasit.rest.model.ScopePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.persistence.NonUniqueResultException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.aura.fasit.rest.model.ResourcePayload.UsedApplicationInstancePayload;

public class Resource2PayloadTransformer extends ToPayloadTransformer<Resource, ResourcePayload> {
    private FasitRepository repo;
    private ApplicationInstanceRepository applicationInstanceRepository;
    private URI baseUri;
    private boolean showUsage;
    private static final Logger log = LoggerFactory.getLogger(Resource2PayloadTransformer.class);

    public Resource2PayloadTransformer(FasitRepository repo, ApplicationInstanceRepository applicationInstanceRepository, URI baseUri) {
        this.repo = repo;
        this.applicationInstanceRepository = applicationInstanceRepository;
        this.baseUri = baseUri;
    }

    public Resource2PayloadTransformer(FasitRepository repo, ApplicationInstanceRepository applicationInstanceRepository, URI baseUri, Long currentRevision) {
        this.repo = repo;
        this.applicationInstanceRepository = applicationInstanceRepository;
        this.baseUri = baseUri;
        this.revision = currentRevision;
    }

    @Override
    protected ResourcePayload transform(Resource from) {
        ResourcePayload resourcePayload = new ResourcePayload(from.getType(), from.getAlias());

        resourcePayload.addLink("self", UriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/resources/{resourceId}")
                .buildAndExpand(from.getID())
                .toUri());
        
        resourcePayload.addLink("revisions", UriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/resources/{resourceId}/revisions")
                .buildAndExpand(from.getID())
                .toUri());

        Scope resourceScope = from.getScope();
        resourcePayload.scope = transform(resourceScope);
        resourcePayload.dodgy = from.isDodgy();
        addPropertiesToResourceElement(from, resourcePayload);
        addSecrets(from, resourcePayload);
        addFiles(from, resourcePayload);

        if (revision != null) {
            resourcePayload.revision = revision;
        }
        if (showUsage) {
            resourcePayload.usedByApplications = addUsedByApplications(from);
            resourcePayload.exposedBy = addExposedByApplication(from).orElse(null);
        }

        if (from.getLifeCycleStatus() != null) {
            resourcePayload.lifeCycleStatus = from.getLifeCycleStatus();
        }

        return resourcePayload;
    }

    private void addPropertiesToResourceElement(Resource resource, ResourcePayload resourcePayload) {
        for (Map.Entry<String, String> entry : resource.getProperties().entrySet()) {
            resourcePayload.addProperty(entry.getKey(), entry.getValue());
        }
    }

    private void addFiles(Resource resource, ResourcePayload resourceElement) {
        for (Map.Entry<String, FileEntity> entry : resource.getFiles().entrySet()) {
            URI fileUri = UriComponentsBuilder.fromUri(baseUri)
                    .path("/api/v2/resources/{resourceId}/file/{filename}")
                    .buildAndExpand(resource.getID(), entry.getKey())
                    .toUri();
            resourceElement.addFile(entry.getKey(), fileUri);
        }
    }

    private void addSecrets(Resource resource, ResourcePayload resourcePayload) {
        for (Map.Entry<String, Secret> entry : resource.getSecrets().entrySet()) {
            resourcePayload.addSecret(entry.getKey(), entry.getValue().getID(), baseUri, entry.getValue().getVaultPath());
        }
    }

    private List<UsedApplicationInstancePayload> addUsedByApplications(Resource resource) {
        return applicationInstanceRepository.findApplicationInstancesUsing(resource).stream().map(this::transform).collect(Collectors.toList());
    }

    private Optional<UsedApplicationInstancePayload> addExposedByApplication(Resource resource) {

        try {
            ApplicationInstance appInstance = applicationInstanceRepository.findApplicationInstanceByExposedResourceId(resource.getID());
            if (appInstance != null) {
                return Optional.of(transform(appInstance));
            }
        } catch (IncorrectResultSizeDataAccessException | NonUniqueResultException exception) {
            log.error("Resource is exposed by more than 1 application. This should not be possible... " + resource.getAlias() + " " + resource.getID());
        }

        return Optional.empty();
    }

    private UsedApplicationInstancePayload transform(ApplicationInstance appInstance) {
        UsedApplicationInstancePayload usedApplicationInstancePayload = new UsedApplicationInstancePayload();
        Environment environment = repo.getEnvironmentBy(appInstance.getCluster());

        usedApplicationInstancePayload.environment = environment.getName();
        usedApplicationInstancePayload.application = appInstance.getApplication().getName();
        usedApplicationInstancePayload.version = appInstance.getVersion();
        usedApplicationInstancePayload.id = appInstance.getID();
        usedApplicationInstancePayload.ref = UriComponentsBuilder.fromUri(baseUri)
                .path("/api/v2/applicationinstances/{id}")
                .buildAndExpand(appInstance.getID())
                .toUri();
        return usedApplicationInstancePayload;
    }

    public static ScopePayload transform(Scope scope) {
        EnvironmentClass envClass = scope.getEnvClass();
        String environmentName = scope.getEnvironmentName();
        Zone zone = null;

        if (scope.getDomain() != null) {
            zone = scope.getDomain().isInZone(Zone.FSS) ? Zone.FSS : Zone.SBS;
        }

        String application = scope.getApplication() != null ? scope.getApplication().getName() : null;

        return new ScopePayload().
                environmentClass(envClass).
                environment(environmentName).
                zone(zone).
                application(application);
    }

    public void setShowUsage(boolean showUsage) {
        this.showUsage = showUsage;
    }
}
