package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.client.ApplicationDO;
import no.nav.aura.envconfig.client.ApplicationGroupDO;
import no.nav.aura.envconfig.client.ApplicationListDO;
import no.nav.aura.envconfig.model.application.Application;
import no.nav.aura.envconfig.model.application.ApplicationGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/conf/applications")
public class ApplicationRestService {
    private final static Logger log = LoggerFactory.getLogger(ApplicationRestService.class);

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
    @GetMapping(path = "/{env}/instances")
    @Deprecated
    public ResponseEntity<?> getApplicationInstancesForEnvironment(@PathVariable(name = "env") String envName) {
        URI redirectTo = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/environments/{env}/applications")
                .buildAndExpand(envName)
                .toUri();       
        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(redirectTo).build();
    }

    /**
     * Henter ut informasjon om en gitt applikasjon
     * 
     * @param appName
     * @return info om applikasjonen
     * 
     * @HTTP 404 hvis applikasjonen ikke finnes i envconfig
     */
    @GetMapping(path = "/{app}", produces = {
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    public ApplicationDO getApplicationInfo(@PathVariable(name = "app") String appName) {
    	log.info("Henter applikasjon " + appName);
        Application app = repo.findApplicationByName(appName);

        if (app == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application " + appName + " not found");
        }

        return new ApplicationDO(app.getName(), app.getGroupId(), app.getArtifactId(), app.getPortOffset());
    }

    /**
     * Henter ut alle applikasjoner som finnes
     * @return liste over alle applikasjoner
     * @HTTP 404 om ingen applikasjoner finnes
     */
    @GetMapping(produces = {
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    public ApplicationListDO getApplications() {
    	log.info("Henter alle applikasjoner");
        Set<Application> apps = repo.getApplications();
        List<ApplicationDO> applications = new ArrayList<>();

        if (apps == null || apps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No applications found");

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
        ApplicationListDO applicationListDO = new ApplicationListDO(applications);
        return applicationListDO;
    }

    private ApplicationGroupDO getApplicationGroup(Application application) {
        ApplicationGroup applicationGroup = repo.findApplicationGroup(application);
        if (applicationGroup != null) {
            return new ApplicationGroupDO(applicationGroup.getName());
        }
        return null;
    }
}
