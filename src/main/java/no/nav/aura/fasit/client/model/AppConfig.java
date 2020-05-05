package no.nav.aura.fasit.client.model;


public class AppConfig {
    
    public enum Format {
        xml, json
    }

    private Format format;
    private String content;

    public AppConfig() {
    }

    public AppConfig(Format format, String content) {
        super();
        this.format = format;
        this.content = content;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
