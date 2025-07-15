package no.nav.aura.fasit.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import no.nav.aura.envconfig.model.secrets.Secret;

@Repository
public interface SecretRepository extends CrudRepository<Secret, Long>{

}
