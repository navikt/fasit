package no.nav.aura.envconfig.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.EnvironmentDO;
import no.nav.aura.envconfig.client.EnvironmentListDO;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

/**
 * API for å håndtere infrastuktur og miljøer
 */
@RestController
@RequestMapping(path = "/conf/environments")
public class EnvironmentRestService {
    @Autowired
    private FasitRepository repo;

    public EnvironmentRestService() {
    }

    public EnvironmentRestService(FasitRepository repo) {
        this.repo = repo;
    }

    /**
     * Finner alle miljøer eller alle miljøer i en gitt miljøklasse
     * 
     * @param envClass
     *            Miljøklassen
     * @return List med envName og envClass
     * 
     */

    @GetMapping(produces = {
            MediaType.APPLICATION_XML_VALUE, 
            MediaType.APPLICATION_JSON_VALUE
    })
    public EnvironmentListDO getEnvironments(@RequestParam(name = "envClass", required = false) String envClass) {
        List<EnvironmentDO> environments = new ArrayList<>();
        for (Environment environment : getEnvironmentsWithScope(envClass)) {
            environments.add(new EnvironmentDO(environment.getName(), environment.getEnvClass().toString(), ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri()));
        }
        EnvironmentListDO environmentList = new EnvironmentListDO(environments);
        return environmentList;
    }

    private Collection<Environment> getEnvironmentsWithScope(String envClass) {
        if (envClass != null) {
            return repo.findEnvironmentsBy(EnvironmentClass.valueOf(envClass.toLowerCase()));
        } else {
            return repo.getEnvironments();
        }
    }

    /**
     * Finner ett miljø fra ett gitt navn
     * 
     * @param environmentName
     * 
     * @HTTP 404 Hvis miljøet ikke finnes
     * 
     */

    @GetMapping(path = "/{environmentName}", produces = {
            MediaType.APPLICATION_XML_VALUE, 
            MediaType.APPLICATION_JSON_VALUE
    })
    public EnvironmentDO getEnvironment(@PathVariable(name = "environmentName") String environmentName) {
        Environment environment = findEnvironment(environmentName);
        return new EnvironmentDO(environment.getName(), environment.getEnvClass().toString(), ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri());

    }

    private Environment findEnvironment(String envName) {
        Environment environment = repo.findEnvironmentBy(envName.toLowerCase());
        if (environment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Environment " + envName + " not found");
        }
        return environment;
    }
}
