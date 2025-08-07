package no.nav.aura.jaxb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import no.nav.aura.appconfig.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXParseException;

@RunWith(Parameterized.class)
public class ValidateAppConfigXmlTest {

    private String fileName;

    public ValidateAppConfigXmlTest(String filename) {
        this.fileName = filename;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { "app-config.xml" }, { "app-config-min.xml" }, { "app-config-max.xml" } };
        return Arrays.asList(data);
    }

    @Test
    public void validateWithXsd() throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema xsd = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream("/appconfig.xsd")));
        Validator validator = xsd.newValidator();

        try {
            validator.validate(new StreamSource(getClass().getResourceAsStream("/" + fileName)));
        } catch (SAXParseException e) {
            fail("file " + fileName + " is not valid in line " + e.getLineNumber() + " " + e.getMessage());
        }

    }

    @Test
    public void parse() throws Exception {
        Application app = Application.instance(getClass().getResourceAsStream("/" + fileName));
        assertNotNull("application for file " + fileName, app);
    }

}
