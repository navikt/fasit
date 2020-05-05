package no.nav.aura.fasit.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.fasit.rest.helpers.LifeCycleSupport;
import no.nav.aura.fasit.rest.model.LifecyclePayload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Component
@Path("api/v1/lifecycle")
public class LifecycleRest {

    @Inject
    FasitRepository fasitRepository;

    @Inject
    private LifeCycleSupport lifeCycleSupport;


    @Context
    private UriInfo uriInfo;

    protected enum EntityType {
        Application(Application.class),
        Environment(Environment.class),
        Resource(Resource.class),
        Cluster(Cluster.class),
        ApplicationInstance(ApplicationInstance.class),
        Node(Node.class);

        private final Class<? extends DeleteableEntity> entityType;

        EntityType(Class<? extends DeleteableEntity> entityType) {
            this.entityType = entityType;
        }

        public Class<? extends DeleteableEntity> entityClass() {
            return entityType;
        }
    }

    public LifecycleRest() {
    }


    /**
     * Changes lifecycle status for entities.
     */
    @PUT
    @Path("{entityType}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public void updateLifecycle(@PathParam("entityType") EntityType entityType, @PathParam("id") Long id, @Valid LifecyclePayload payload) {
        Class<? extends DeleteableEntity> aClass = entityType.entityClass();

        try {
            DeleteableEntity entity = fasitRepository.getById(aClass, id);
            lifeCycleSupport.update(entity, payload);
        } catch (NoResultException nre) {
            throw new NotFoundException("Unable to find entity " + id + " of type " + entityType.toString());
        }
    }
}
