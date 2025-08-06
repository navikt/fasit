package no.nav.aura.fasit.repository;

import java.util.Collection;
import java.util.List;

import jakarta.inject.Inject;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Cluster;
import no.nav.aura.envconfig.model.infrastructure.Node;
import no.nav.aura.envconfig.spring.SpringOraclePerformanceTestConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

@SpringJUnitConfig(classes = {SpringOraclePerformanceTestConfig.class})
@Transactional
// @Rollback
@Disabled
public abstract class NodeRepositoryPerformanceTest {

    @Inject
    NodeRepository nodeRepository;

    @Autowired
    private FasitRepository repository;

    private long startTime;

    @BeforeEach
    public void setup() throws Exception {
        startTime = System.currentTimeMillis();
    }

    @AfterEach
    public void done() {
        System.out.println("Time: " + (System.currentTimeMillis() - startTime));
    }

    // @Test
    public void getAllNodeRepo() {
        nodeRepository.findAll();
        System.out.println("NodeRepo");
    }

    @Test
    public void getAllNodeRepo2() {
        List<Node> nodes = nodeRepository.findAllNodes();
        System.out.print((System.currentTimeMillis() - startTime) + " ");
        for (Node node : nodes) {

            Optional<Cluster> cluster = FluentIterable.from(node.getClusters()).first();
            if (cluster.isPresent() && !cluster.get().getApplications().isEmpty()) {
                Collection<Application> applications = cluster.get().getApplications();
            }
            nodeRepository.findEnvironment(node);

        }
        System.out.println("NodeRepo findAllNode spesial");
    }

    // @Test
    public void getAllFasitRepo() {
        repository.getAll(Node.class);
        System.out.println("FasitRepo getall");
    }
}
