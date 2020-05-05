package no.nav.aura.envconfig.rest;

import static io.restassured.RestAssured.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.Response.Status;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.resource.FileEntity;
import no.nav.aura.envconfig.model.resource.Resource;
import no.nav.aura.envconfig.model.resource.ResourceType;
import no.nav.aura.envconfig.model.resource.Scope;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class FileRestServiceTest extends RestTest {

    @Test
    public void downloadFile() {
        Resource resource = createResource();
        byte[] bs = expect().statusCode(Status.OK.getStatusCode())
                .header("Content-Disposition", equalTo("attachment; filename=\"fila.bin\""))
                .when().get(FileRestService.createPath(resource, "keystore")).asByteArray();
        assertThat(bs, equalTo(createFile()));
    }

    @Test
    public void testGetFile_wrongIdFormat() {
        assert404Error("/conf/files/54235423", "Unable to find file");
        assert404Error("/conf/files/kremfjes-47328", "Unable to find file");
        assert404Error("/conf/files/login-a473843", "Unable to find file");
        assert404Error("/conf/files/resourceproperties-473843", "Unable to find file");
        assert404Error("/conf/files/resourceproperties-" + createResource().getID() + "-dill", "Unable to find file");
    }

    private void assert404Error(String path, String errorMessage) {
        expect().statusCode(Status.NOT_FOUND.getStatusCode())
                .body(Matchers.containsString(errorMessage))
                .with().get(path);
    }

    private Resource createResource() {
        Resource resource = new Resource("minRessurs", ResourceType.Certificate, new Scope(EnvironmentClass.u));
        resource.putFileAndValidate("keystore", new FileEntity("fila.bin", new ByteArrayInputStream(createFile())));
        resource.putSecretAndValidate("keystorepassword", "secretClearText");
        resource.putPropertyAndValidate("keystorealias", "mjau");
        resource = repository.store(resource);
        return resource;
    }

    private byte[] createFile() {
        byte[] bs = new byte[256];
        for (int i = 0; i < bs.length; i++) {
            bs[i] = (byte) i;
        }
        return bs;
    }
}
