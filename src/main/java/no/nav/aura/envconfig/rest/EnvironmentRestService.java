package no.nav.aura.envconfig.rest;

import com.google.common.collect.Sets;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.EnvironmentDO;
import no.nav.aura.envconfig.model.infrastructure.Environment;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import javax.ws.rs.NotFoundException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.Set;

/**
 * API for å håndtere infrastuktur og miljøer
 */
@Path("/conf/environments")
@Component
public class EnvironmentRestService {

    @Inject
    private FasitRepository repo;

    @Context
    private UriInfo uriInfo;

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

    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Set<EnvironmentDO> getEnvironments(@QueryParam("envClass") String envClass) {
        Set<EnvironmentDO> environments = Sets.newHashSet();
        for (Environment environment : getEnvironmentsWithScope(envClass)) {
            environments.add(new EnvironmentDO(environment.getName(), environment.getEnvClass().toString(), uriInfo.getBaseUriBuilder()));
        }
        return environments;
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

    @GET
    @Path("/{environmentName}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public EnvironmentDO getEnvironment(@PathParam("environmentName") String environmentName) {
        Environment environment = findEnvironment(environmentName);
        return new EnvironmentDO(environment.getName(), environment.getEnvClass().toString(), uriInfo.getBaseUriBuilder());

    }

    private Environment findEnvironment(String envName) {
        Environment environment = repo.findEnvironmentBy(envName.toLowerCase());
        if (environment == null) {
            throw new NotFoundException("Environment " + envName + " not found");
        }
        return environment;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

}
