package no.nav.aura.fasit.repository;

import org.springframework.data.repository.CrudRepository;

import no.nav.aura.envconfig.model.secrets.Secret;

public interface SecretRepository extends CrudRepository<Secret, Long>{

}
