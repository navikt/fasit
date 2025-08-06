package no.nav.aura.fasit.rest;

import jakarta.validation.Valid;


import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

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

@RestController
@RequestMapping(path = "/api/v1/lifecycle")
public class LifecycleRest {

    @Inject
    FasitRepository fasitRepository;

    @Inject
    private LifeCycleSupport lifeCycleSupport;


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
        
        public static EntityType fromString(String value) {
            for (EntityType type : values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No enum constant for " + value);
        }
    }

    public LifecycleRest() {
    }


    /**
     * Changes lifecycle status for entities.
     */
    @PutMapping(path = "/{entityType}/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void updateLifecycle(@PathVariable(name = "entityType") String entityTypeStr, @PathVariable(name = "id") Long id, @Valid @RequestBody LifecyclePayload payload) {
        EntityType entityType = EntityType.fromString(entityTypeStr);
        Class<? extends DeleteableEntity> aClass = entityType.entityClass();

        try {
            DeleteableEntity entity = fasitRepository.getById(aClass, id);
            lifeCycleSupport.update(entity, payload);
        } catch (NoResultException nre) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find entity " + id + " of type " + entityType.toString());
        }
    }
}
