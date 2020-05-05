package no.nav.aura.envconfig.model.resource;

public class ResourceTypeDocumentationText {

    public static final String CONFLUENCE_DMGR_URL = "http://confluence.adeo.no/x/DowfB";
    public static final String CONFLUENCE_DATAPOWER_URL = "http://confluence.adeo.no/x/DowfB";
    public static final String CONFLUENCE_ROLEMAPPING_URL = "http://confluence.adeo.no/x/DowfB";
    public static final String CONFLUENCE_REST_URL = "http://confluence.adeo.no/x/7q_aBQ";
    public static final String CONFLUENCE_EMAILADRESS_URL = "http://confluence.adeo.no/x/46_aBQ";
    public static final String CONFLUENCE_CERTIFICATE_URL = "http://confluence.adeo.no/x/7osfB";
    public static final String CONFLUENCE_CREDENTIALS_URL = "http://confluence.adeo.no/x/7osfB";
    public static final String CONFLUENCE_BASEURL_URL = "http://confluence.adeo.no/x/g6_aBQ";
    public static final String CONFLUENCE_LOADBALANCER_URL = "http://confluence.adeo.no/x/thlBB";
    public static final String CONFLUENCE_SMTPSERVER_URL = "http://confluence.adeo.no/x/mfNuB";
    public static final String CONFLUENCE_APPLICATIONPROPERTIES_URL = "http://confluence.adeo.no/x/tp1JB";
    public static final String CONFLUENCE_CICS_URL = "http://confluence.adeo.no/x/qmxBB";
    public static final String CONFLUENCE_JMS_URL = "http://confluence.adeo.no/x/NowfB";
    public static final String CONFLUENCE_QUEUEMANAGER_URL = "http://confluence.adeo.no/x/NowfB";
    public static final String CONFLUENCE_WEBSERVICEGATEWAY_URL = "http://confluence.adeo.no/x/4YsfB";
    public static final String CONFLUENCE_WEBSERVICEENDPOINT_URL = "http://confluence.adeo.no/x/4YsfB";
    public static final String CONFLUENCE_SOAPSERVICEENDPOINT_URL = "http://confluence.adeo.no/x/4YsfB";
    public static final String CONFLUENCE_EJB_URL = "http://confluence.adeo.no/x/J0BRBQ";
    public static final String CONFLUENCE_OPENAM_URL = "http://confluence.adeo.no/x/EkAXB";
    public static final String CONFLUENCE_OIDC_URL = "http://confluence.adeo.no/x/8iBaD";
    public static final String CONFLUENCE_LDAP_URL = "http://confluence.adeo.no/x/aX1BB";
    public static final String CONFLUENCE_DATASOURCE_URL = "http://confluence.adeo.no/x/6osfB";
    public static final String CONFLUENCE_MEMORY_URL = "http://confluence.adeo.no/x/YDVNB";
    public static final String CONFLUENCE_CHANNEL_URL = "http://confluence.adeo.no/x/glhPCg";

    protected static String getDocumentationForApplicationProperties() {
        StringBuilder description = new StringBuilder("Noe ganger er det bruk for å kunne sette spesifikke properties som endrer seg fra miljø til miljø som ikke passer inn i noen av de andre kategoriene. ");
        description.append("Typisk bruk av denne ressursen vil være å skru på loggnivå.\n");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        return description.toString();
    }

    protected static String getDocumentationForBaseUrl() {
        StringBuilder description = new StringBuilder("BaseUrl er for å linke til andre tjenester og skal være på formen protokoll://server:port. \n");
        description.append("BaseUrler skal ikke innholde noe etter \"port\" dersom dette er likt i alle miljøer.\n");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        description.append("Navnestandard alias:  Hvis den er spesifikk for applikasjonen, prefixes den med applikasjonsnavnet. ");

        return description.toString();
    }

    protected static String getDocumentationForCertificate() {
        return "Mangler dok";
    }

    protected static String getDocumentationForChannel() {
        StringBuilder description = new StringBuilder("For kanal \n");
        description.append("\n");
        description.append("Scoping: Defineres typisk innenfor miljø, domene, miljøklasse og applikasjon.\n");
        description.append("Navne-konvensjon for name: Miljønavn_Applikasjonsnavn. \n");
        return description.toString();
    }

    protected static String getDocumentationForCics() {
        StringBuilder description = new StringBuilder("En container-managed ressursadapter på server som gjøres tilgjengelig for applikasjonen via jndi \n");
        description.append("\n");
        description.append("Eksponering: JNDI \n");

        return description.toString();
    }

    protected static String getDocumentationForCredentials() {
        StringBuilder description = new StringBuilder("For brukernavn og passord \n");
        description.append("\n");
        description.append("Scoping: Defineres typisk innenfor miljø, domene og miljøklasse.\n");
        description.append("Navne-konvensjon for alias: Hvis den er spesifikk for applikasjonen, prefixes den med applikasjonsnavnet. \n");
        return description.toString();
    }

    protected static String getDocumentationForDatasource() {
        StringBuilder description = new StringBuilder("Managed JNDI datasource på server. Kan også inneholde settings for connection pool. \n");
        description.append("\n");
        description.append("Eksponering: JNDI\n");
        description.append("Scoping: Bør spesifiseres med miljøklasse, miljø, domene og applikasjon\n");
        description.append("Navnestandard for alias: Prefixes med applikasjonsnavn");
        return description.toString();
    }

    protected static String getDocumentationForDatapower() {
        return "Mangler dok";
    }

    protected static String getDocumentationForDeploymentManager() {

        StringBuilder description = new StringBuilder("Scoping: Bør spesifiseres med miljøklasse, miljø, domene  \n");
        return description.toString();
    }

    protected static String getDocumentationForEjb() {
        StringBuilder description = new StringBuilder(
                "Brukes til å definere opp ejb-tjenester som tilbys/benyttes i applikasjonen. Disse vil bli automatisk opprettet i env-config ved deploy av en applikasjon som har definert \"exposed-services\". \n");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        return description.toString();
    }

    protected static String getDocumentationForEmailAddress() {
        StringBuilder description = new StringBuilder("Brukes til å definere opp miljøavhengige til- og fra-epostadresser for applikasjonene. \n ");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        description.append("Navnestandard alias: Hvis den er spesifikk for applikasjonen, prefixes den med applikasjonsnavnet.");
        return description.toString();
    }

    protected static String getDocumentationForLDAP() {
        StringBuilder description = new StringBuilder("Applikasjonen kan konfigurere ldap som en ressurs som tilgjengeliggj�res som system properties på applikasjonsserveren.\n");
        description.append("Ved deploy vil systemet forsøke å finne  en ressurs i Fasit som matcher alias og type(LDAP)");
        return description.toString();
    }

    protected static String getDocumentationForLoadBalancer() {
        return "Mangler dok";
    }

    protected static String getDocumentationForOpenAm() {
        return "Mangler dok";
    }

    protected static String getDocumentationForOpenIdConnect() {
        StringBuilder description = new StringBuilder("Applikasjonen kan konfigurere OpenIdConnect som " +
                "sikkerhetsmodul som vil opprette ISSO agent i OpenAM. Denne ressursen vil så kunne brukes for å " +
                "hente ut tokens fra OpenAM"); return description.toString();
    }

    protected static String getDocumentationForQueues() {
        StringBuilder description = new StringBuilder("Køer konfigureres på serveren\n");
        description.append("Køer og kanaler bestilles slik at de eksisterer på MQ-serveren. Dette skjer ikke automatisk\n");
        description.append("\n");
        description.append("Ved deploy av myApp til t1 blir følgende generert: \n");
        description.append("ChannelName: T1_MYAPP \n");
        return description.toString();
    }

    protected static String getDocumentationForMemoryParameters() {
        StringBuilder description = new StringBuilder("Minne parametere som angis i tekststrenger i fasit. Disse må også angis som tatt i bruk i app-config.\n");
        description.append("Dette kan våre minneinnstilliger for JVM, docker eller andre prosesser/ressurser som trenger slike innstillinger. \n");
        description.append("Settes som tall med størrelsestype, eksempelvis: 1g eller 1024m. \n");
        description.append("\n");
        description.append("Ved deploy av applikasjon vil innstillingene settes for applikasjonen denne er scopet til. Hvis de ikke er satt vil man benytte default oppsett (utregnet fra serverstørrelse). \n");
        return description.toString();
    }

    protected static String getDocumentationForQueueManager() {
        StringBuilder description = new StringBuilder("Brukes for å gi en kømanager og køer konfigurert via JNDI på serveren.\n ");
        description.append("Kømanageren er en ressurs i fasit, mens prefix på kønavn genereres basert på miljø man deployer til. \n");
        description.append("\n");
        description.append("Eksponering: JNDI\n");
        description.append("Navnestandard alias: mqGateway");
        return description.toString();
    }

    protected static String getDocumentationForRestService() {

        StringBuilder description = new StringBuilder(
                "Brukes til å definere opp rest-tjenester som tilbys/benyttes i applikasjonen. Disse vil bli automatisk opprettet i env-config ved deploy av en applikasjon som har definert \"exposed-services\". \n");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        return description.toString();

    }

    protected static String getDocumentationForRoleMapping() {
        return "Mangler dok";
    }

    protected static String getDocumentationForSMTPServer() {
        StringBuilder description = new StringBuilder("Brukes til � definere opp epostservere som brukes i applikasjonen. \n");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        description.append("Scoping: Defineres innenfor domene og miljøklasse for T og oppover. Mer spesifikt i utviklingsmiljøer, der man gjerne angir både miljø og applikasjon.\n");
        description.append("Navnestandard alias: \"smtp\"");
        return description.toString();
    }

    protected static String getDocumentationForUrl() {

        StringBuilder description = new StringBuilder(
                "Brukes til å definere opp url-er som tilbys i applikasjonen. Disse vil bli automatisk opprettet i env-config ved deploy av en applikasjon som har definert \"exposed-services\". \n");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        return description.toString();

    }

    protected static String getDocumentationForWebserviceGateway() {
        return "Mangler dok";
    }

    protected static String getDocumentationForWebserviceEndpoint() {

        StringBuilder description = new StringBuilder(
                "Brukes til � definere opp web-tjenester som benyttes i applikasjonen. Disse vil bli automatisk opprettet i env-config ved deploy av en applikasjon som har definert \"exposed-services\". \n");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        description.append("Scoping: Defineres typisk innenfor miljø, domene og miljøklasse.\n");
        description.append("Navnestandard alias:  Kortnavn på tjenesten. Se linker under:");
        return description.toString();
    }

    protected static String getDocumentationForSoapService() {

        StringBuilder description = new StringBuilder(
                "Brukes til å definere opp web-tjenester som benyttes i applikasjonen. Disse vil bli automatisk opprettet i env-config ved deploy av en applikasjon som har definert \"exposed-services\". \n\nForskjellen på denne ressurstypen kontra Webservice endpoint er at denne ikke plukkes opp av provisjoneringen av tjenester til service gateway");
        description.append("\n");
        description.append("Eksponering: System properties \n");
        description.append("Scoping: Defineres typisk innenfor miljø, domene og miljøklasse.\n");
        description.append("Navnestandard alias:  Kortnavn på tjenesten. Se linker under:");
        return description.toString();
    }

    public static String getDocumentationForLoadBalancerConfig() {
        return "Mangler dok";
    }
    
    public static String docForFileLibrary() {
        return "Mangler dok";
    }

    public static String getDocumentationForAzureOIDC() {
        StringBuilder description = new StringBuilder("Bruker AzureAD sin OIDC pålogging");
        return description.toString();
    }
}
