package no.nav.aura.envconfig.rest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.ApplicationDO;
import no.nav.aura.envconfig.client.ApplicationGroupDO;
import no.nav.aura.envconfig.client.ApplicationGroupListDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;

@RestController
@RequestMapping("/conf/applicationGroups")
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
    @GetMapping(
            value = "/{applicationGroup}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
        )
    public ResponseEntity<ApplicationGroupDO> getApplicationGroupInfo(@PathVariable("applicationGroup") String applicationGroupName) {
        ApplicationGroup applicationGroup = repo.findApplicationGroupByName(applicationGroupName);

        if (applicationGroup == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application group " + applicationGroupName + " not found");

        }

        return ResponseEntity.ok(
                new ApplicationGroupDO(applicationGroup.getName(), transformApplications(applicationGroup.getApplications()))
            );
    }

    private Set<ApplicationDO> transformApplications(Set<Application> applications) {
    	return applications.stream()
			.map(application -> new ApplicationDO(application.getName(), application.getGroupId(), application.getArtifactId(), application.getPortOffset()))
			.collect(Collectors.toSet());
    }

    /**
     * Finds all application groups
     * 
     * @return List with evry application group
     * @HTTP 404 if no application groups found
     */
    @GetMapping(
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
        )
    public ApplicationGroupListDO getApplicationGroups() {
        final Set<ApplicationGroup> applicationGroups = repo.getApplicationGroups();

        if (applicationGroups == null || applicationGroups.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No applications found");

        }

        List<ApplicationGroupDO> result = applicationGroups.stream()
				.map(applicationGroup -> new ApplicationGroupDO(
						applicationGroup.getName(),
						transformApplications(applicationGroup.getApplications())))
				.collect(Collectors.toList());

        ApplicationGroupListDO applicationGroupList = new ApplicationGroupListDO(result);
        return applicationGroupList;

    }
}
