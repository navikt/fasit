package no.nav.aura.fasit.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import no.nav.aura.envconfig.model.resource.Resource;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

}
