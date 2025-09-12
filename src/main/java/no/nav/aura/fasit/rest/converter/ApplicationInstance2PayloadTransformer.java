package no.nav.aura.fasit.rest.converter;

import no.nav.aura.envconfig.model.infrastructure.*;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.fasit.repository.ApplicationInstanceRepository;
import no.nav.aura.fasit.rest.ApplicationInstanceRest;
import no.nav.aura.fasit.rest.ResourceRest;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload.MissingResourcePayload;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload.NodeRefPayload;
import no.nav.aura.fasit.rest.model.ApplicationInstancePayload.ResourceRefPayload;
import no.nav.aura.fasit.rest.model.Link;
import no.nav.aura.fasit.rest.model.PortPayload;
import org.joda.time.DateTime;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static no.nav.aura.fasit.rest.ClusterRest.clusterUrl;

public class ApplicationInstance2PayloadTransformer extends ToPayloadTransformer<ApplicationInstance, ApplicationInstancePayload> {

    private URI baseUri;
    private ApplicationInstanceRepository applicationInstanceRepository;
    private Boolean showUsage = true;

    public ApplicationInstance2PayloadTransformer(URI baseUri, ApplicationInstanceRepository applicationInstanceRepository) {
        this.baseUri = baseUri;
        this.applicationInstanceRepository = applicationInstanceRepository;
    }

    public ApplicationInstance2PayloadTransformer(URI baseUri, ApplicationInstanceRepository applicationInstanceRepository, Long currentRevision) {
        this.baseUri = baseUri;
        this.revision = currentRevision;
        this.applicationInstanceRepository = applicationInstanceRepository;
    }

    @Override
    protected ApplicationInstancePayload transform(final ApplicationInstance instance) {
        ApplicationInstancePayload payload = new ApplicationInstancePayload(showUsage);
        payload.addLink("self", UriBuilder.fromUri(baseUri).path(ApplicationInstanceRest.class).path(ApplicationInstanceRest.class, "getApplicationInstance").build(instance.getID()));
        payload.addLink("revisions", UriBuilder.fromUri(baseUri).path(ApplicationInstanceRest.class).path(ApplicationInstanceRest.class, "getRevisions").build(instance.getID()));

        final String selfTestPath = instance.getSelftestPagePath();

        payload.application = instance.getApplication().getName();
        payload.version = instance.getVersion();
        payload.nodes = toNodeRefs(instance.getPorts());
        payload.selftest = selfTestPath;


        Environment environment = applicationInstanceRepository.findEnvironmentWith(instance);
        payload.accessControl.environmentClass = environment.getEnvClass();
        payload.environment = environment.getName();
        payload.environmentClass = environment.getEnvClass();
        Cluster cluster = instance.getCluster();


        if (revision != null) {
            payload.revision = revision;
        }
        if (cluster != null) {
            payload.cluster = new Link(cluster.getName(), clusterUrl(baseUri, environment, cluster));
        }

        payload.selfTestUrls = addSelfTestUrls(selfTestPath, cluster, instance.getHttpsPort());

        if (showUsage) {
            for (ExposedServiceReference exposedServiceReference : instance.getExposedServices()) {
                payload.exposedresources.add(createResourceRefPayload(exposedServiceReference));
            }

            instance.getResourceReferences().stream()
                    .filter(rr -> !rr.isFuture())
                    .forEach(rr -> payload.usedresources.add(createResourceRefPayload(rr)));

            instance.getResourceReferences().stream()
                    .filter(rr -> rr.isFuture())
                    .forEach(rr -> payload.missingresources.add(createMissingPayload(rr)));
        }
        if (revision != null) {
            payload.appconfig = new ApplicationInstancePayload.AppconfigPayload(UriBuilder.fromUri(baseUri).path(ApplicationInstanceRest.class).path("{id}/revisions/" + revision + "/appconfig").build(instance.getID()));
        } else {
            payload.appconfig = new ApplicationInstancePayload.AppconfigPayload(UriBuilder.fromUri(baseUri).path(ApplicationInstanceRest.class).path("{id}/appconfig").build(instance.getID()));
        }
        return payload;

    }

    private Set<String> addSelfTestUrls(String selfTestPath, Cluster cluster, int httpsPort){
        Set<String> selfTestUrls = new HashSet();

        if(selfTestPath != null) {
            String loadBalancerUrl = cluster.getLoadBalancerUrl();

            if (loadBalancerUrl != null) {
                selfTestUrls.add(normalize( loadBalancerUrl + "/" + selfTestPath));
            }
            selfTestUrls.addAll(cluster.getNodes()
                    .stream()
                    .map(n -> normalize(format("https://%s:%d/%s", n.getHostname(), httpsPort, selfTestPath)))
                    .collect(Collectors.toList()));
        }

        return selfTestUrls;
    }

    private String normalize(String url) {
        try {
            return new URI(url).normalize().toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    protected Set<NodeRefPayload> toNodeRefs(Set<Port> ports) {
        Set<String> uniqueHostnames = ports.stream().map(p -> p.hostname).collect(toSet());

        return uniqueHostnames.stream().map(hostname -> {
            Stream<Port> portsForHostname = ports.stream().filter(p -> p.hostname.equalsIgnoreCase(hostname));
            Set<PortPayload> portPayloads = portsForHostname.map(p -> new PortPayload(p.number, PortPayload.PortType.valueOf(p.type))).collect(toSet());
            return new NodeRefPayload(hostname, portPayloads);
        }).collect(toSet());
    }

    private MissingResourcePayload createMissingPayload(ResourceReference rr) {
        return new MissingResourcePayload(rr.getAlias(), rr.getResourceType());
    }

    private ResourceRefPayload createResourceRefPayload(ResourceReference resourceRef) {
        ResourceRefPayload payload = new ResourceRefPayload();
        payload.alias = resourceRef.getAlias();
        payload.revision = resourceRef.getRevision();

        Resource resource = resourceRef.getResource();
        if (resource != null) {
            payload.id = resource.getID();
            payload.scope = Resource2PayloadTransformer.transform(resource.getScope());
            payload.type = resource.getType();
            DateTime updated = resource.getUpdated();
            if(updated != null ) {
                payload.lastChange = updated.getMillis();
            }

            payload.lastUpdateBy = resource.getUpdatedBy();
            // TODO med revision
            payload.ref = UriBuilder.fromUri(baseUri).path(ResourceRest.class).path(ResourceRest.class, "getResource").build(resource.getID());
        } else {
            payload.deleted = true;
        }
        return payload;
    }

    private ResourceRefPayload createResourceRefPayload(ExposedServiceReference exposed) {
        ResourceRefPayload payload = new ResourceRefPayload();
        payload.alias = exposed.getResourceAlias();
        Resource exposedResource = exposed.getResource();
        payload.id = exposedResource.getID();
        payload.scope = Resource2PayloadTransformer.transform(exposedResource.getScope());
        payload.type = exposedResource.getType();
        payload.revision = exposed.getRevision();
        payload.lastChange = exposedResource.getUpdated().getMillis();
        payload.lastUpdateBy = exposedResource.getUpdatedBy();
        payload.ref = UriBuilder.fromUri(baseUri).path(ResourceRest.class).path(ResourceRest.class, "getResource").build(exposedResource.getID());

        return payload;
    }

    public void setShowUsage(Boolean showUsage) {
        this.showUsage = showUsage;
    }
}
