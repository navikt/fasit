package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.ExposedServiceReference;
import no.nav.aura.envconfig.model.infrastructure.Port;
import no.nav.aura.envconfig.model.infrastructure.ResourceReference;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.fasit.repository.ResourceRepository;
import no.nav.aura.fasit.repository.RevisionRepository;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload.MissingResourcePayload;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload.ResourceRefPayload;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class Payload2ApplicationInstanceTransformer extends FromPayloadTransformer<ApplicationInstancePayload, ApplicationInstance> {

    private static final Logger log = LoggerFactory.getLogger(Payload2ApplicationInstanceTransformer.class);

    private Optional<ApplicationInstance> existing;
    private ResourceRepository resourceRepository;
    private RevisionRepository revisionRepository;

    public Payload2ApplicationInstanceTransformer(ApplicationInstance existing, ResourceRepository resourceRepository, RevisionRepository revisionRepository) {
        this.resourceRepository = resourceRepository;
        this.revisionRepository = revisionRepository;
        this.existing = Optional.ofNullable(existing);
    }


    @Override
    protected ApplicationInstance transform(ApplicationInstancePayload from) {

        ApplicationInstance instance = existing.orElseThrow(() -> new IllegalArgumentException("Existing application instance can not be null"));

        // TODO ta dato med i payload?
        instance.setDeployDate(DateTime.now());
        instance.setVersion(from.version);

        optional(from.selftest).ifPresent(p -> instance.setSelftestPagePath(p));
        optional(from.appconfig).ifPresent(p -> instance.setAppconfigXml(p.value));

        Set<ResourceReference> usedResources = transformUsedResources(from.usedresources);
        Set<ResourceReference> futureResources = transformMissingResources(from.missingresources);
        instance.setResourceReferences(concat(usedResources, futureResources));

        instance.setExposedServices(transformExposed(from.exposedresources));

        Set<Port> ports = transformPorts(from.nodes);
        instance.setPorts(ports);

        return instance;
    }

    private static Set<Port> transformPorts(Set<ApplicationInstancePayload.NodeRefPayload> nodes) {
        return nodes.stream()
                .map(n -> n.ports.stream().map(p -> new Port(n.hostname, p.port, p.type.name())).collect(toSet()))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    private Set<ExposedServiceReference> transformExposed(Set<ResourceRefPayload> exposedServices) {
        return exposedServices.stream()
                .map(new revisionNumberEnricher())
                .map(resourceRef -> new ExposedServiceReference(resourceRepository.getReferenceById(resourceRef.id), resourceRef.revision))
                .collect(toSet());
    }

    private Set<ResourceReference> concat(Set<ResourceReference> usedResources, Set<ResourceReference> futureResources) {
        return Stream.concat(usedResources.stream(), futureResources.stream())
                .collect(toSet());
    }

    private Set<ResourceReference> transformMissingResources(Set<MissingResourcePayload> missingResources) {
        return missingResources.stream()
                .map(missingResource -> ResourceReference.future(missingResource.alias, missingResource.type))
                .collect(toSet());
    }

    private Set<ResourceReference> transformUsedResources(Set<ResourceRefPayload> resourceRefs) {
        return resourceRefs.stream()
                .map(new revisionNumberEnricher())
                .map(resourceRef -> {
                    Resource one = resourceRepository.getReferenceById(resourceRef.id);
                    return new ResourceReference(one, resourceRef.revision);
                })
                .collect(toSet());
    }

    private final class revisionNumberEnricher implements Function<ResourceRefPayload, ResourceRefPayload> {
        @Override
        public ResourceRefPayload apply(ResourceRefPayload ref) {
            if (ref.revision == null) {
                ref.revision = revisionRepository.currentRevision(Resource.class, ref.id);
            }
            return ref;
        }
    }

}
