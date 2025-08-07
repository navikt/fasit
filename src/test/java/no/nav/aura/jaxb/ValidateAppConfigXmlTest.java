package no.nav.aura.jaxb;

import no.nav.aura.appconfig.Application;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class ValidateAppConfigXmlTest {

    private String fileName;

    public void initValidateAppConfigXmlTest(String filename) {
        this.fileName = filename;
    }

    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { "app-config.xml" }, { "app-config-min.xml" }, { "app-config-max.xml" } };
        return Arrays.asList(data);
    }

    @MethodSource("data")
    @ParameterizedTest
    public void validateWithXsd(String filename) throws Exception {
        initValidateAppConfigXmlTest(filename);
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema xsd = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream("/appconfig.xsd")));
        Validator validator = xsd.newValidator();

        try {
            validator.validate(new StreamSource(getClass().getResourceAsStream("/" + fileName)));
        } catch (SAXParseException e) {
            fail("file " + fileName + " is not valid in line " + e.getLineNumber() + " " + e.getMessage());
        }

    }

    @MethodSource("data")
    @ParameterizedTest
    public void parse(String filename) throws Exception {
        initValidateAppConfigXmlTest(filename);
        Application app = Application.instance(getClass().getResourceAsStream("/" + fileName));
        assertNotNull(app, "application for file " + fileName);
    }

}
