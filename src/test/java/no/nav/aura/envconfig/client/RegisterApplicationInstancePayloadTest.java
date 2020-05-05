package no.nav.aura.envconfig.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import no.nav.aura.fasit.client.model.AppConfig;
import no.nav.aura.fasit.client.model.AppConfig.Format;
import no.nav.aura.fasit.client.model.ExposedResource;
import no.nav.aura.fasit.client.model.MissingResource;
import no.nav.aura.fasit.client.model.RegisterApplicationInstancePayload;
import no.nav.aura.fasit.client.model.UsedResource;

import org.junit.jupiter.api.Test;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;

public class RegisterApplicationInstancePayloadTest {

    @Test
    public void generateJsonFromObject() throws ProcessingException, IOException {
        RegisterApplicationInstancePayload payload = new RegisterApplicationInstancePayload("app", "version", "environment");
        payload.setNodes(Arrays.asList("node1", "node2"));
        payload.setUsedResources(new HashSet<>(Arrays.asList((new UsedResource(123, 4534545)))));
        payload.setMissingResources(Arrays.asList(new MissingResource("alais", ResourceTypeDO.BaseUrl)));
        Map<String, String> properties = new HashMap<>();
        properties.put("key", "value");
        payload.setExposedResources(Arrays.asList(new ExposedResource(ResourceTypeDO.WebserviceEndpoint.name(), "myAlias", properties)));
        payload.setAppConfig(new AppConfig(Format.xml, "<xml> ass </xml>"));
        System.out.println(payload.toJson());

        JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();

        ProcessingReport validation = validator.validate(JsonLoader.fromResource("/registerApplicationInstanceSchema.json"), JsonLoader.fromString(payload.toJson()));
        assertTrue(validation.isSuccess());
    }

}
