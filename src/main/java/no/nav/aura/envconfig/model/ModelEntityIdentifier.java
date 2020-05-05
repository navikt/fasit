package no.nav.aura.envconfig.model;

import java.io.Serializable;
import java.util.List;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.auditing.FasitRevision;
import no.nav.aura.envconfig.util.Producer;
import no.nav.aura.envconfig.util.SerializableFunction;
import no.nav.aura.envconfig.util.Tuple;

import org.hibernate.envers.RevisionType;

import com.google.common.base.Optional;

@SuppressWarnings("serial")
public class ModelEntityIdentifier<T extends ModelEntity, DT extends ModelEntity> implements Serializable {

    private final Class<T> entityClass;
    private final Optional<Long> entityId;
    private final Optional<Long> revisionId;
    private final Producer<T> createFunction;
    private final Optional<ModelEntityIdentifier<DT, ?>> dependentIdentifier;

    public ModelEntityIdentifier(Class<T> entityClass, Optional<Long> entityId, Optional<Long> revisionId) {
        this(entityClass, entityId, revisionId, Optional.<Producer<T>> absent(), Optional.<ModelEntityIdentifier<DT, ?>> absent());
    }

    public ModelEntityIdentifier(Class<T> entityClass, Optional<Long> entityId, Optional<Long> revisionId, Optional<Producer<T>> createFunction, Optional<ModelEntityIdentifier<DT, ?>> dependentIdentifier) {
        this.entityClass = entityClass;
        this.entityId = entityId;
        this.revisionId = revisionId;
        this.dependentIdentifier = dependentIdentifier;
        this.createFunction = createFunction.or(new Producer<T>() {
            public T get() {
                try {
                    return ModelEntityIdentifier.this.entityClass.getConstructor(new Class[] {}).newInstance();
                } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                    throw new RuntimeException("Unable to instantiate class " + ModelEntityIdentifier.this.entityClass, e);
                }
            }
        });
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public Optional<Long> getEntityId() {
        return entityId;
    }

    public Optional<Long> getRevisionId() {
        return revisionId;
    }

    public boolean isNew() {
        return !entityId.isPresent();
    }

    public boolean isHead(FasitRepository repository) {
        if (!entityId.isPresent() || !revisionId.isPresent()) {
            return true;
        }
        List<Tuple<Long, RevisionType>> history = repository.getRevisionsFor(entityClass, entityId.get());
        // Is last revision and not DEL (cause they aint head; they be dead; dead like them dodos)
        boolean lastRevision = history.get(0).fst.equals(revisionId.get());
        boolean notDeleted = history.get(0).snd != RevisionType.DEL;
        return lastRevision && notDeleted;
    }

    public T getModelEntity(final FasitRepository repository) {
        if (isHead(repository)) {
            return entityId.transform(new SerializableFunction<Long, T>() {
                public T process(Long id) {
                    return repository.getById(entityClass, id);
                }
            }).or(createFunction);
        }
        FasitRevision<T> revision = repository.getRevision(entityClass, entityId.get(), revisionId.get());
        T modelEntity = revision.getModelEntity();
        if (modelEntity == null) {
            // If the entity is null we have a deleted entity and have to fetch the previous version
            List<Tuple<Long, RevisionType>> history = repository.getRevisionsFor(entityClass, entityId.get());
            if (history.size() < 2){
                throw new RuntimeException(String.format("Deleted entity with missing history, id = %s entityClass = %s", entityId, entityClass));
            }
            return repository.getRevision(entityClass, entityId.get(), history.get(1).fst).getModelEntity();
        }
        return modelEntity;
    }

    @Override
    public String toString() {
        return "Identifier(" + entityClass.getSimpleName() + ", id: " + entityId + ", rev: " + revisionId + ", dependent: " + dependentIdentifier + ")";
    }

    public ModelEntityIdentifier<T, DT> createFunction(Producer<T> createFunction) {
        return new ModelEntityIdentifier<>(entityClass, entityId, revisionId, Optional.of(createFunction), dependentIdentifier);
    }

    public ModelEntityIdentifier<T, DT> revision(long revisionId) {
        return new ModelEntityIdentifier<>(entityClass, entityId, Optional.of(revisionId), Optional.of(createFunction), dependentIdentifier);
    }

    public <O extends ModelEntity> ModelEntityIdentifier<T, O> dependentIdentifier(ModelEntityIdentifier<O, ?> dependentIdentifier) {
        return new ModelEntityIdentifier<T, O>(entityClass, entityId, revisionId, Optional.of(createFunction), Optional.<ModelEntityIdentifier<O, ?>> of(dependentIdentifier));
    }

    public ModelEntityIdentifier<DT, ?> getDependentIdentifier() {
        return dependentIdentifier.get();
    }
}
