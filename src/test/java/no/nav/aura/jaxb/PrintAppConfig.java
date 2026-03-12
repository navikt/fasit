package no.nav.aura.jaxb;

import java.io.StringReader;
import java.io.StringWriter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import no.nav.aura.appconfig.Application;
import no.nav.aura.appconfig.resource.Directory;
import no.nav.aura.appconfig.resource.NfsMount;
import org.junit.jupiter.api.Test;


public class PrintAppConfig {

    @Test
    public void test() {
        Application application = new Application();
        Directory dir = new Directory("name", "proeprty", false);
        dir.setMountOnNfs(new NfsMount("external"));
        application.getResources().add(dir);
        //printXml(application);
    }

    /**
     * Useful utility method for printing the XML when debugging unmarshalling app-config.xml
     * */
    public void printXml(Application app) {
        JAXBContext context;
        try {
            context = JAXBContext.newInstance(Application.class);
            final Marshaller marshaller = context.createMarshaller();
            StringWriter xml = new StringWriter();
            marshaller.marshal(app, xml);
            System.out.println(prettyFormat(xml.toString(), 4));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private String prettyFormat(String input, int indent) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter writer = new StringWriter();
            StreamResult xmlOutput = new StreamResult(writer);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);

            return xmlOutput.getWriter().toString();

        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            throw new RuntimeException("Error when formatting XML", e);
        }
    }

}
