package no.nav.aura.fasit.rest.helpers;

import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.infrastructure.Zone;
import no.nav.aura.fasit.repository.ApplicationRepository;
import no.nav.aura.fasit.repository.EnvironmentRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Optional;

@Component
public class ValidationHelpers {

    @Inject
    private ApplicationRepository applicationRepository;

    @Inject
    EnvironmentRepository environmentRepository;

    public Application getApplication(String applicationName) {
        Application application = applicationRepository.findByNameIgnoreCase(applicationName);

        if (application == null) {
            throw new BadRequestException("Application " + applicationName + " does not exist in fasit");
        }
        return application;
    }

    public Application findApplication(String applicationName) {
        Application application = applicationRepository.findByNameIgnoreCase(applicationName);

        if (application == null) {
            throw new NotFoundException("Application " + applicationName + " does not exist in fasit");
        }
        return application;
    }


    public Environment getEnvironment(String environmentName) {
        Environment environment = environmentRepository.findByNameIgnoreCase(environmentName);

        if (environment == null) {
            throw new BadRequestException(" Environment " + environmentName + " does not exist in fasit");
        }
        return environment;
    }

    public Environment findEnvironment(String environmentName) {
        Environment environment = environmentRepository.findByNameIgnoreCase(environmentName);

        if (environment == null) {
            throw new NotFoundException(" Environment " + environmentName + " does not exist in fasit");
        }
        return environment;
    }

    public Optional<Environment> getOptionalEnvironment(String environmentName) {
        if(environmentName != null ) {
            Environment environment = getEnvironment(environmentName);
            return Optional.of(environment);

        }
        return Optional.empty();
    }

    public Optional<Application> getOptionalApplication(String applicationName) {
        if(applicationName != null ) {
            Application application = getApplication(applicationName);
            return Optional.of(application);

        }
        return Optional.empty();
    }

    public Optional<Domain> domainFromZone(EnvironmentClass environmentClass, Optional<Environment> environment, Zone zone) {
        if (zone != null) {
            if (environmentClass == null && !environment.isPresent()) {
                throw new BadRequestException("Zone without environmentClass or environment is not possible");
            }

            if (environment.isPresent()) {
                environmentClass = environment.get().getEnvClass();
            }
            return Optional.of(Domain.from(environmentClass, zone));
        }
        return Optional.empty();
    }
}
