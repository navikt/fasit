package no.nav.aura.envconfig.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import no.nav.aura.fasit.client.model.AppConfig;
import no.nav.aura.fasit.client.model.AppConfig.Format;
import no.nav.aura.fasit.client.model.ExposedResource;
import no.nav.aura.fasit.client.model.MissingResource;
import no.nav.aura.fasit.client.model.RegisterApplicationInstancePayload;
import no.nav.aura.fasit.client.model.UsedResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class RegisterApplicationInstancePayloadTest {

    @Test
    public void generateJsonFromObject() throws IOException {
        RegisterApplicationInstancePayload payload = new RegisterApplicationInstancePayload("app", "version", "environment");
        payload.setNodes(Arrays.asList("node1", "node2"));
        payload.setUsedResources(new HashSet<>(Arrays.asList((new UsedResource(123, 4534545)))));
        payload.setMissingResources(Arrays.asList(new MissingResource("alais", ResourceTypeDO.BaseUrl)));
        Map<String, String> properties = new HashMap<>();
        properties.put("key", "value");
        payload.setExposedResources(Arrays.asList(new ExposedResource(ResourceTypeDO.WebserviceEndpoint.name(), "myAlias", properties)));
        payload.setAppConfig(new AppConfig(Format.xml, "<xml> ass </xml>"));
        System.out.println(payload.toJson());

        SchemaRegistry factory = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4);
        InputStream schemaStream = getClass().getResourceAsStream("/registerApplicationInstanceSchema.json");
        Schema schema = factory.getSchema(schemaStream);

        JsonMapper mapper = new JsonMapper();
        JsonNode jsonNode = mapper.readTree(payload.toJson());
        List<Error> validationResult = schema.validate(jsonNode);

        assertTrue(validationResult.isEmpty(), "JSON Schema validation failed: " + validationResult);
    }

}
