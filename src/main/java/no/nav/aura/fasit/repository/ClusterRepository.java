package no.nav.aura.fasit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {
    
    @Query("select e from Environment e JOIN e.clusters c where c=:cluster")
    Environment findEnvironment(@Param("cluster") Cluster cluster);
}
