package no.nav.aura.sensu;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class SensuClient {

    private static Logger log = LoggerFactory.getLogger(SensuClient.class);
    private final String sensuHost;
    private String environmentName;
    private int sensuPort;

    public SensuClient(
            String environmentName,
            int sensuPort,
            String sensuHostname
    ) {
        this.environmentName = environmentName;
        this.sensuPort = sensuPort;
        this.sensuHost = sensuHostname;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void sendEvent(String measurement, Map tags, Map fields) {
        try {
            final String dataPoint = toLineProtocol(measurement, addDefaultTags(tags), fields);
            String sensuEvent = createSensuEvent(measurement, dataPoint);
            writeToSocket(sensuEvent);
            log.debug("Sent event with output {} to InfluxDB via sensu-client", dataPoint);
        } catch (RuntimeException e) {
            log.error("Unable to send event to InfluxDB via sensu-client", e);
        }
    }

    private Map<String, Object> addDefaultTags(Map<String, Object> tags) {
        Map<String, Object> map = new HashMap<>();
        map.putAll(tags);
        map.put("application", "fasit");
        map.put("environment", environmentName);
        map.put("hostname", getHostname());
        return map;
    }

    protected static String createSensuEvent(String eventName, String output) {
        return "{\"name\":\"" + eventName + "\",\"type\":\"metric\",\"handlers\":[\"events\"],\"output\":\"" + output + "\"}";
    }

    protected static String toLineProtocol(String measurement, Map<String, Object> tags, Map<String, Object> fields) {
        return String.format("%s%s %s %d", measurement, tags != null ? "," + toCSV(tags) : "", transformFields(fields), System.currentTimeMillis() / 1000);
    }

    protected static String transformFields(Map<String, Object> fields) {
        if (fields != null) {
            String fieldsString = "";
            for (Map.Entry<String, Object> field : fields.entrySet()) {
                String key = field.getKey();
                Object value = field.getValue();
                if (value instanceof String) {
                    fieldsString += "," + key + "=" + escape((String) value);
                } else {
                    fieldsString += "," + key + "=" + value;
                }
            }
            return fieldsString.substring(1);
        } else {
            throw new RuntimeException("InfluxDB datapoint fields can't be null!");
        }
    }

    private static String escape(String string) {
        return "\\\"" + string + "\\\"";
    }

    private static String toCSV(Map<String, Object> map) {
        if (map != null) {
            return Joiner.on(",").withKeyValueSeparator("=").join(map);
        } else {
            return "";
        }
    }

    private void writeToSocket(String data) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(sensuHost, sensuPort), 1000);

            OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            osw.write(data, 0, data.length());
            osw.flush();
            log.debug("Wrote {} to socket with port {}", data, sensuPort);
        } catch (ConnectException e) {
            // for å slippe full stacktrace i enhetstester mm.
            log.error("Unable to connect to {}:{} {}", sensuHost, sensuPort, e.getMessage());
        } catch (IOException e) {
            log.error("Unable to write data {} to socket with port {}", data, sensuPort, e);
        }
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Unknown host " + e);
            return "Unknown host";
        }
    }
}