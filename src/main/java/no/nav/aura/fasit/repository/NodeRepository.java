package no.nav.aura.fasit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Node;

public interface NodeRepository extends JpaRepository<Node, Long>, JpaSpecificationExecutor<Node> {

    @Query("select c from Cluster c JOIN c.nodes n where n=:node")
    Cluster findClusterByNode(@Param("node") Node node);

    @Query("select n from Node n JOIN FETCH n.password")
    List<Node> findAllNodes();

    @Query("select e from Environment e JOIN e.nodes n where n=:node")
    Environment findEnvironment(@Param("node") Node node);

    @Query("select n from Node n where exists (select e from Environment e join e.nodes en where n = lower(en) and e.name = lower(:environmentName))")
    List<Node> findNodesByEnvironmentName(@Param("environmentName") String environmentName);

    @Query("select n from Node n where exists (select e from Environment e join e.nodes en where n = lower(en) and e.envClass = :envClass)")
    List<Node> findNodesByEnvironmentClass(@Param("envClass") EnvironmentClass envClass);

    @Query("select n from Node n where lower(n.hostname)= lower(:hostname)")
    Node findNodeByHostName(@Param("hostname") String hostname);
    
//    @Query("select n from Node n where lower(n.hostname) LIKE %lower(:hostname)%")
    List<Node> findByHostnameContainingIgnoreCase(@Param("hostname") String hostname);

    @Query("select n from Node n where exists (select ai from ApplicationInstance ai where lower(ai.application.name) = lower(:application) and ai.cluster MEMBER OF n.clusters)")
//    @Query("select ai from ApplicationInstance ai, Environment env where lower(ai.application.name) = lower(:application) and ai.cluster MEMBER OF env.clusters and lower(env.name) = lower(:environment)")
    List<Node> findNodeByApplication(@Param("application") String app);

}
