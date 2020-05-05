package no.nav.aura.envconfig.rest;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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
import javax.ws.rs.core.MediaType;
import java.util.Set;

@Path("/conf/applicationGroups")
@Component
public class ApplicationGroupRestService {

    @Autowired
    private FasitRepository repo;

    public ApplicationGroupRestService() {
    }

    public ApplicationGroupRestService(FasitRepository repo) {
        this.repo = repo;
    }

    /**
     * Lists all applications in an application group
     * 
     * @param applicationGroupName
     * @return info om applikasjonen
     * @HTTP 404 hvis applikasjonen ikke finnes i envconfig
     */
    @GET
    @Path("/{applicationGroup}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ApplicationGroupDO getApplicationGroupInfo(@PathParam("applicationGroup") String applicationGroupName) {
        ApplicationGroup applicationGroup = repo.findApplicationGroupByName(applicationGroupName);

        if (applicationGroup == null) {
            throw new NotFoundException("Application group " + applicationGroupName + " not found");
        }

        return new ApplicationGroupDO(applicationGroup.getName(), transformApplications(applicationGroup.getApplications()));
    }

    private Set<ApplicationDO> transformApplications(Set<Application> applications) {
        return Sets.newHashSet(Collections2.transform(applications, new Function<Application, ApplicationDO>() {
            public ApplicationDO apply(Application input) {
                return new ApplicationDO(input.getName(), input.getGroupId(), input.getArtifactId(), input.getPortOffset());
            }
        }));
    }

    /**
     * Finds all application groups
     * 
     * @return List with evry application group
     * @HTTP 404 if no application groups found
     */
    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Set<ApplicationGroupDO> getApplicationGroups() {
        final Set<ApplicationGroup> applicationGroups = repo.getApplicationGroups();

        if (applicationGroups == null || applicationGroups.isEmpty()) {
            throw new NotFoundException("No applications found");
        }

        return Sets.newHashSet(Collections2.transform(applicationGroups,
                new Function<ApplicationGroup, ApplicationGroupDO>() {
                    public ApplicationGroupDO apply(ApplicationGroup applicationGroup) {
                        return new ApplicationGroupDO(
                                applicationGroup.getName(),
                                transformApplications(applicationGroup.getApplications()));
                    }
                }));
    }
}
