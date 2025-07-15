package no.nav.aura.fasit.repository;

import no.nav.aura.envconfig.model.infrastructure.ApplicationInstance;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.resource.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationInstanceRepository extends JpaRepository<ApplicationInstance, Long>, JpaSpecificationExecutor<ApplicationInstance> {

    @Query("select a from ApplicationInstance a JOIN a.resourceReferences rr where rr.resource = :resource")
    List<ApplicationInstance> findApplicationInstancesUsing(@Param("resource") Resource resource);
    
    @Query("select count(a.id) from ApplicationInstance a JOIN a.resourceReferences rr where rr.resource = :resource")
    int countApplicationInstancesUsing(@Param("resource") Resource resource);

    @Query("select ai from ApplicationInstance ai, Environment env where lower(ai.application.name) = lower(:application) and ai.cluster MEMBER OF env.clusters and lower(env.name) = lower(:environment)")
    ApplicationInstance findInstanceOfApplicationInEnvironment(@Param("application") String applicationName, @Param("environment") String environmentName);
    
    @Query("select env from  Environment env, ApplicationInstance ai where ai =:instance and ai.cluster MEMBER OF env.clusters")
    Environment findEnvironmentWith(@Param("instance") ApplicationInstance instance);

    @Query("select ai from ApplicationInstance ai join ai.exposedServices es join es.resource res where res.id = :resourceId")
    ApplicationInstance findApplicationInstanceByExposedResourceId(@Param("resourceId") Long resourceId);

    @Query("select ai from ApplicationInstance ai join ai.exposedServices es join es.resource res where res.id = :resourceId")
    List<ApplicationInstance> findAllApplicationInstancesExposingSameResource(@Param("resourceId") Long resourceId);

}
