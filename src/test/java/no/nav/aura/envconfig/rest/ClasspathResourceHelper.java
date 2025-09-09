package no.nav.aura.envconfig.rest;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClasspathResourceHelper {
    public static String getStringFromFileOnClassPath(String path) {
        try {
            URL payloadFile = ApplicationInstanceResourceSpringTest.class.getResource(path);
            if (payloadFile == null) {
                throw new RuntimeException("Unable to find classpath URL for resource, " + path);
            }
            return new String(Files.readAllBytes(Path.of(payloadFile.toURI())), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch resource, " + path + " from classpath");
        }
    }
}
