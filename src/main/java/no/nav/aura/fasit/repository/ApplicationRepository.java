package no.nav.aura.fasit.repository;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    Application findByNameIgnoreCase(String name);
}