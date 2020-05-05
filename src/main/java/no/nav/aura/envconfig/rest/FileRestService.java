package no.nav.aura.envconfig.rest;

import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.resource.FileEntity;
import no.nav.aura.envconfig.model.resource.Resource;
import javax.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;

/**
 * Api for å håndtere filer fra envconfig
 */
@Component
@Path("/conf/files")
public class FileRestService {

    private final FasitRepository repo;
    private static Logger log = LoggerFactory.getLogger(FileRestService.class);

    @Autowired
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
    @GET
    @Path("/{id}")
    @Produces("application/octet-stream")
    public Response get(@PathParam("id") String combinedId) {
        String[] parts = combinedId.split("-");
        if (parts.length != 3 || !combinedId.matches("resourceproperties-[0-9]+-[^-]+")) {
            throw new NotFoundException("Unable to find file for id " + combinedId);
        }
        Resource resource = repo.getById(Resource.class, Integer.valueOf(parts[1]));
        log.info("Getting file with property {} from resource with id {}", parts[2], parts[1]);
        FileEntity fileEntity = resource.getFiles().get(parts[2]);
        if (fileEntity == null) {
            throw new NotFoundException("Unable to find file for property name " + parts[2]);
        }
        return Response
                .ok(new ByteArrayInputStream(fileEntity.getFileData()))
                .header("Content-Disposition", "attachment; filename=\"" + fileEntity.getName() + "\"")
                .build();
    }

    public static String createPath(Resource resource, String propertyName) {
        return "/conf/files/resourceproperties-" + resource.getID() + "-" + propertyName;
    }

}
