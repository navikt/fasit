package no.nav.aura.envconfig.client.adapter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB adapter for ZonedDateTime to ensure proper XML serialization/deserialization
 */
public class ZonedDateTimeAdapter extends XmlAdapter<String, ZonedDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public ZonedDateTime unmarshal(String value) {
        return value != null ? ZonedDateTime.parse(value, FORMATTER) : null;
    }

    @Override
    public String marshal(ZonedDateTime value) {
        return value != null ? value.format(FORMATTER) : null;
    }
}