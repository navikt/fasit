package no.nav.aura.envconfig.client;

/*
 * This enum is a duplicate of PlatformType in fasit-domain to avoid mixing domain packages and client api
 * Changes in this file has to be reflected in the other file aswell
 * */
public enum PlatformTypeDO {
    JBOSS(8443, 0, 8080),
    WILDFLY(8443, 0, 8080),
    WAS(9443, 9810, 9080),
    WAS9(9443, 9810, 9080),
    LIBERTY(9443, 9810, 9080),
    BPM(9443, 0, 9080),
    BPM86(9443, 0, 9080),
    DOCKER(8443, 0, 8080),
    DATAPOWER_PHYSICAL(0, 0, 0),
    DATAPOWER_VIRTUAL(0, 0, 0),
    OPENAM_SERVER(8443, 0, 0),
    OPENAM_PROXY(80, 0, 0),
    WINDOWS(0, 0, 0),
    WINDOWS_TERMINALSERVER(0, 0, 0),
    WINDOWS_APPSERVER(0, 0, 0),
    WINDOWS_IISSERVER(0, 0, 0),
    WINDOWS_RPTSERVER(0, 0, 0);

    private int httpsPort;
    private int baseBootstrapPort;
    private int httpPort;

    private PlatformTypeDO(int baseHttpsPort, int baseBootstrapPort, int httpPort) {
        this.httpsPort = baseHttpsPort;
        this.baseBootstrapPort = baseBootstrapPort;
        this.httpPort = httpPort;
    }

    public int getBaseHttpsPort() {
        return httpsPort;
    }

    public int getBaseBootstrapPort() {
        return baseBootstrapPort;
    }

    public int getBaseHttpPort() {
        return httpPort;
    }
}
