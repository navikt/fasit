package no.nav.aura.envconfig.rest;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;

public class ClasspathResourceHelper {
    public static String getStringFromFileOnClassPath(String path) {
        try {
            URL payloadFile = ApplicationInstanceResourceSpringTest.class.getResource(path);
            if (payloadFile == null) {
                throw new RuntimeException("Unable to find classpath URL for resource, " + path);
            }
            return Resources.toString(payloadFile, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to fetch resource, " + path + " from classpath");
        }
    }
}
