package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Flyway settings for data source
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class FlywaySettings {

    @XmlAttribute(required = false)
    private String baselineVersion;

    @XmlAttribute(required = false)
    private String encoding;

    public FlywaySettings() {
    }

    /**
     * @param baselineVersion
     *            - Specifies the baseline version for the database.
     * @param encoding
     *            - Specifies the character encoding of the migration.
     * 
     */
    public FlywaySettings(String baselineVersion, String encoding) {
        this.baselineVersion = baselineVersion;
        this.encoding = encoding;
    }

    public String getBaselineVersion() {
        return baselineVersion;
    }

    public void setBaselineVersion(String baselineVersion) {
        this.baselineVersion = baselineVersion;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}