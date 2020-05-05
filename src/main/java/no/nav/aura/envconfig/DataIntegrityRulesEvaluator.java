package no.nav.aura.envconfig;

import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.util.Result;

import java.util.Objects;

public class DataIntegrityRulesEvaluator {

    private final FasitRepository repository;

    public DataIntegrityRulesEvaluator(FasitRepository repository) {
        this.repository = repository;
    }

    public void checkConsistency(ModelEntity entity) {
        Result result = Result.ok();
        if (entity instanceof ApplicationInstance) {
            result = onlyOneApplicationPrEnvironment(repository, (ApplicationInstance) entity);
        }
        if (entity instanceof Cluster) {
            result = uniqueClusterNameInEnvironment(repository, (Cluster) entity);
        }
        if (!result.isOk()) {
            throw new IllegalArgumentException(result.getMessage());
        }

    }

    private static Result uniqueClusterNameInEnvironment(FasitRepository repository, Cluster cluster) {
        if (!cluster.isNew()) {
            Environment environment = repository.getEnvironmentBy(cluster);
            Cluster storedcluster = environment.findClusterByName(cluster.getName());
            if (storedcluster != null && isDifferentInstance(cluster, storedcluster)) {
                return Result.error("Cluster " + cluster.getName() + " already exist in environment " + environment.getName());
            }
        }
        return Result.ok();

    }

    private static Result onlyOneApplicationPrEnvironment(FasitRepository repository, ApplicationInstance applicationInstance) {
        if (!applicationInstance.getCluster().isNew()) {
            Environment environment = repository.getEnvironmentBy(applicationInstance.getCluster());
            ApplicationInstance existingInstance = environment.findApplicationByName(applicationInstance.getApplication().getName());
            if (existingInstance != null && isDifferentInstance(existingInstance, applicationInstance)) {
                return Result.error("Application " + applicationInstance.getApplication().getName() + " already exists in environment " + environment.getName());
            }
        }
        return Result.ok();
    }

    public static boolean isDifferentInstance(ModelEntity instanceToCheck, ModelEntity storedInstance) {
        return isDifferentInstance(storedInstance.getID(), instanceToCheck.getID());
    }

    public static boolean isDifferentInstance(Long storedId, Long id) {
        return !Objects.equals(storedId, id);
    }

}
