package no.nav.aura.envconfig.model.infrastructure;

/*
 * This enum is a duplicate of PlatformTypeDO in fasit-client-api to avoid mixing domain packages and client api
 * Changes in this file has to be reflected in the other file aswell
 * */

public enum PlatformType {
    JBOSS(8443, 0),
    WILDFLY(8443, 0),
    WAS(9443, 9810),
    WAS9(9443, 9810),
    LIBERTY(9443, 9810),
    BPM(9443, 0),
    BPM86(9443, 0),
    DOCKER(8443, 0),
    DATAPOWER_PHYSICAL(0, 0),
    DATAPOWER_VIRTUAL(0, 0),
    OPENAM_SERVER(8443, 0),
    OPENAM_PROXY(80, 0),
    WINDOWS(0, 0),
    WINDOWS_TERMINALSERVER(0, 0),
    WINDOWS_APPSERVER(0, 0),
    WINDOWS_IISSERVER(0, 0),
    WINDOWS_RPTSERVER(0, 0);

    private int httpsPort;
    private int baseBootstrapPort;

    private PlatformType(int baseHttpsPort, int baseBootstrapPort) {
        this.httpsPort = baseHttpsPort;
        this.baseBootstrapPort = baseBootstrapPort;
    }

    public int getBaseHttpsPort() {
        return httpsPort;
    }

    public int getBaseBootstrapPort() {
        return baseBootstrapPort;
    }
}
