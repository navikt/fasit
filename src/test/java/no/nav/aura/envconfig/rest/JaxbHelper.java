package no.nav.aura.envconfig.rest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

public class JaxbHelper {
    static byte[] marshal(Object object) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        jaxbContext.createMarshaller().marshal(object, baos);
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T unmarshal(InputStream stream, Class<T> clazz) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        return (T) jaxbContext.createUnmarshaller().unmarshal(stream);
    }

}
