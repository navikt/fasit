package no.nav.aura.fasit.repository;

import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EnvironmentRepository extends JpaRepository<Environment, Long>, JpaSpecificationExecutor<Environment> {

    List<Environment> findByEnvClass(EnvironmentClass environmentClass);
    
    Environment findByNameIgnoreCase(String name);
}
