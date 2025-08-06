package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.resource.FileEntity;
import no.nav.aura.envconfig.model.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;

/**
 * Api for å håndtere filer fra envconfig
 */
@RestController
@RequestMapping(path = "/conf/files")
public class FileRestService {

    private final FasitRepository repo;
    private static Logger log = LoggerFactory.getLogger(FileRestService.class);

    public FileRestService(FasitRepository repo) {
        this.repo = repo;
    }

    /**
     * Last ned en fil
     * 
     * @param combinedId
     *            id til fila på format 'resourceproperties-[0-9]+-[^-]+'
     * @return fila som en stream
     * 
     * @HTTP 404 ved ugyldig id eller fil ikke funnet
     */
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> get(@PathVariable(name = "id") String combinedId) {
        String[] parts = combinedId.split("-");
        if (parts.length != 3 || !combinedId.matches("resourceproperties-[0-9]+-[^-]+")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find file for id " + combinedId);
        }
        Resource resource = repo.getById(Resource.class, Integer.valueOf(parts[1]));
        log.info("Getting file with property {} from resource with id {}", parts[2], parts[1]);
        FileEntity fileEntity = resource.getFiles().get(parts[2]);
        if (fileEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find file for property name " + parts[2]);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition
                .attachment()
                .filename(fileEntity.getName())
                .build());
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(new ByteArrayInputStream(fileEntity.getFileData())));
    }

    public static String createPath(Resource resource, String propertyName) {
        return "/conf/files/resourceproperties-" + resource.getID() + "-" + propertyName;
    }

}
