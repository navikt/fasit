package no.nav.aura.envconfig.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import no.nav.aura.appconfig.Application;
import no.nav.aura.envconfig.client.rest.ResourceElement;

import org.junit.jupiter.api.Test;

public class DeployedApplicationDOTest {

    @Test
    public void test() throws Exception {
        DeployedApplicationDO deployedApplication = new DeployedApplicationDO(new Application(), "1.0");
        deployedApplication.addUsedResources(new ResourceElement(ResourceTypeDO.BaseUrl, "myUrlAlias"));
        deployedApplication.addUsedResources(new ResourceElement("DataSource", "myDB"));

        assertEquals(2, deployedApplication.getUsedResources().size());

        JAXBContext jaxbContext = JAXBContext.newInstance(DeployedApplicationDO.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(deployedApplication, writer);
        String result = writer.getBuffer().toString();
        // System.out.println(result);
        assertTrue(result.contains("myUrlAlias"), "resoure in marshaled result");

    }
}
