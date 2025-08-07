package no.nav.aura.envconfig.rest;

import com.google.common.collect.Sets;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.ApplicationDO;
import no.nav.aura.envconfig.client.ApplicationGroupDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import javax.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Set;

@Path("/conf/applications")
@Component
public class ApplicationRestService {

        @Context
    private UriInfo uriInfo;

    @Autowired
    private FasitRepository repo;

    public ApplicationRestService() {
    }

    public ApplicationRestService(FasitRepository repo) {
        this.repo = repo;
    }

    /**
     * @param envName
     * @return versjonsinfo om alle applikasjoner i et miljø
     * @deprecated use environments/{env}/applications
     */
    @GET
    @Path("/{env}/instances")
    @Deprecated
    public Response getApplicationInstancesForEnvironment(@PathParam("env") String envName) {
        URI redirectTo = uriInfo.getBaseUriBuilder().clone().path("environments/{env}/applications").build(envName);
        return Response.temporaryRedirect(redirectTo).build();
    }

    /**
     * Henter ut informasjon om en gitt applikasjon
     * 
     * @param appName
     * @return info om applikasjonen
     * 
     * @HTTP 404 hvis applikasjonen ikke finnes i envconfig
     */
    @GET
    @Path("/{app}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ApplicationDO getApplicationInfo(@PathParam("app") String appName) {
        Application app = repo.findApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException("Application " + appName + " not found");
        }

        return new ApplicationDO(app.getName(), app.getGroupId(), app.getArtifactId(), app.getPortOffset());
    }

    /**
     * Henter ut alle applikasjoner som finnes
     * @return liste over alle applikasjoner
     * @HTTP 404 om ingen applikasjoner finnes
     */
    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Set<ApplicationDO> getApplications() {
        Set<Application> apps = repo.getApplications();
        Set<ApplicationDO> applications = Sets.newHashSet();

        if (apps == null || apps.isEmpty()) {
            throw new NotFoundException("No applications found");
        }

        for (Application application : apps) {
            ApplicationDO applicationDO = new ApplicationDO(
                    application.getName(),
                    application.getGroupId(),
                    application.getArtifactId(),
                    application.getPortOffset());
            applicationDO.setApplicationGroup(getApplicationGroup(application));
            applications.add(applicationDO);

        }
        return applications;
    }

    private ApplicationGroupDO getApplicationGroup(Application application) {
        ApplicationGroup applicationGroup = repo.findApplicationGroup(application);
        if (applicationGroup != null) {
            return new ApplicationGroupDO(applicationGroup.getName());
        }
        return null;
    }
}
