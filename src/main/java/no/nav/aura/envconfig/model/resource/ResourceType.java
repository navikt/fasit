package no.nav.aura.envconfig.model.resource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static no.nav.aura.envconfig.model.resource.PropertyField.*;
import static no.nav.aura.envconfig.model.resource.PropertyField.Type.FILE;
import static no.nav.aura.envconfig.model.resource.PropertyField.Type.SECRET;
import static no.nav.aura.envconfig.model.resource.PropertyField.ValidationType.*;

public enum ResourceType {
    // START SNIPPET: resource-type-definitions
    DataSource(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForDatasource(), ResourceTypeDocumentationText.CONFLUENCE_DATASOURCE_URL),
            text("url"),
            text("onsHosts").optional(),
            text("oemEndpoint").optional(),
            text("username"),
            secret("password")),
    MSSQLDataSource(
            new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForDatasource(), ResourceTypeDocumentationText.CONFLUENCE_DATASOURCE_URL),
            text("url"),
            text("schema"),
            text("username"),
            secret("password")),
    DB2DataSource(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForDatasource(), ResourceTypeDocumentationText.CONFLUENCE_DATASOURCE_URL),
            text("hostname"),
            text("port"),
            text("dbaname"),
            text("username"),
            secret("password"),
            text("schema")),
    LDAP(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForLDAP(), ResourceTypeDocumentationText.CONFLUENCE_LDAP_URL),
            text("url").validate(LDAP_URL),
            text("username"),
            secret("password"),
            text("domain").optional(),
            text("basedn").optional(),
            text("user.basedn").optional(),
            text("serviceuser.basedn").optional()),
    BaseUrl(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForBaseUrl(), ResourceTypeDocumentationText.CONFLUENCE_BASEURL_URL),
            text("url")),
    Credential(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForCredentials(), ResourceTypeDocumentationText.CONFLUENCE_CREDENTIALS_URL),
            text("username").optional(),
            secret("password")),
    Certificate(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForCertificate(), ResourceTypeDocumentationText.CONFLUENCE_CERTIFICATE_URL),
            file("keystore"),
            secret("keystorepassword"),
            text("keystorealias")),
    OpenAm(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForOpenAm(), ResourceTypeDocumentationText.CONFLUENCE_OPENAM_URL),
            text("restUrl").validate(HTTP_URL),
            text("logoutUrl").validate(HTTP_URL),
            text("hostname"),
            text("username"),
            secret("password")),
    OpenIdConnect(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForOpenIdConnect(),
            ResourceTypeDocumentationText.CONFLUENCE_OIDC_URL),
            text("agentName").validate(HTTP_URL),
            secret("password"),
            text("hostUrl").validate(HTTP_URL),
            text("issuerUrl").validate(HTTP_URL),
            text("jwksUrl").validate(HTTP_URL)),
    Cics(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForCics(), ResourceTypeDocumentationText.CONFLUENCE_CICS_URL),
            text("cicsname"),
            text("url"),
            text("port")),
    RoleMapping(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForRoleMapping(),
            ResourceTypeDocumentationText.CONFLUENCE_ROLEMAPPING_URL),
            text("groups")),
    QueueManager(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForQueueManager(), ResourceTypeDocumentationText.CONFLUENCE_QUEUEMANAGER_URL),
            text("name"),
            text("hostname"),
            text("port")),
    WebserviceEndpoint(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForWebserviceEndpoint(), ResourceTypeDocumentationText.CONFLUENCE_WEBSERVICEENDPOINT_URL),
            text("endpointUrl").validate(HTTP_URL),
            text("wsdlUrl").validate(HTTP_URL).optional(),
            enumeration("securityToken", SecurityToken.class),
            text("description").optional()),
    SoapService(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForSoapService(), ResourceTypeDocumentationText.CONFLUENCE_SOAPSERVICEENDPOINT_URL),
            text("endpointUrl").validate(HTTP_URL),
            text("wsdlUrl").validate(HTTP_URL).optional(),
            enumeration("securityToken", SecurityToken.class),
            text("description").optional()),
    RestService(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForRestService(), ResourceTypeDocumentationText.CONFLUENCE_REST_URL),
            text("url").validate(HTTP_URL),
            text("description").optional()),
    WebserviceGateway(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForWebserviceGateway(), ResourceTypeDocumentationText.CONFLUENCE_WEBSERVICEGATEWAY_URL),
            text("url").validate(HTTP_URL)),
    EJB(new ResourceTypeDocumentation(
            ResourceTypeDocumentationText.getDocumentationForEjb(), ResourceTypeDocumentationText.CONFLUENCE_EJB_URL),
            text("providerUrl").validate(URL_LIST),
            text("jndi").optional(),
            text("beanHomeInterface").optional(),
            text("beanComponentInterface").optional(),
            text("description").optional()),
    Datapower(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForDatasource(), ResourceTypeDocumentationText.CONFLUENCE_DATAPOWER_URL),
            text("adminurl").validate(HTTP_URL),
            text("adminweburl").validate(HTTP_URL),
            text("username"),
            secret("password")),
    EmailAddress(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForEmailAddress(), ResourceTypeDocumentationText.CONFLUENCE_EMAILADRESS_URL),
            text("address").validate(EMAIL)),
    SMTPServer(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForSMTPServer(), ResourceTypeDocumentationText.CONFLUENCE_SMTPSERVER_URL),
            text("host"),
            text("port")),
    Queue(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForQueues(), ResourceTypeDocumentationText.CONFLUENCE_JMS_URL),
            text("queueName"),
            text("queueManager").optional()),
    DeploymentManager(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForDeploymentManager(), ResourceTypeDocumentationText.CONFLUENCE_DMGR_URL),
            text("hostname"),
            text("username"),
            secret("password")),
    ApplicationProperties(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForApplicationProperties(), ResourceTypeDocumentationText.CONFLUENCE_APPLICATIONPROPERTIES_URL),
            text("applicationProperties")),
    MemoryParameters(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForMemoryParameters(), ResourceTypeDocumentationText.CONFLUENCE_MEMORY_URL),
            text("minMemory"),
            text("maxMemory"),
            text("permGenMemory").optional()),
    LoadBalancer(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForLoadBalancer(), ResourceTypeDocumentationText.CONFLUENCE_LOADBALANCER_URL),
            text("hostname"),
            text("secondary_hostname"),
            text("username"),
            secret("password")),
    LoadBalancerConfig(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForLoadBalancerConfig(), ResourceTypeDocumentationText.CONFLUENCE_LOADBALANCER_URL),
            text("url"),
            text("poolName"),
            text("contextRoots").optional()),
    FileLibrary(new ResourceTypeDocumentation(ResourceTypeDocumentationText.docForFileLibrary(), ResourceTypeDocumentationText.CONFLUENCE_LOADBALANCER_URL),
            text("path"),
            text("nodes")),
    Channel(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForChannel(), ResourceTypeDocumentationText.CONFLUENCE_CHANNEL_URL),
            text("name").validate("[A-Z_0-9]+"),
            text("queueManager").optional()),
    AzureOIDC(new ResourceTypeDocumentation(ResourceTypeDocumentationText.getDocumentationForAzureOIDC(), ResourceTypeDocumentationText.CONFLUENCE_OIDC_URL),
            text("discoveryUri"),
            text("clientId"),
            secret("clientSecret"),
            text("callbackUri"));
    // END SNIPPET: resource-type-definitions

    private final ResourceTypeDocumentation documentation;
    private final List<PropertyField> propertyFields;

    private ResourceType(ResourceTypeDocumentation documentation, PropertyField... properties) {
        this.documentation = documentation;
        propertyFields = Lists.newArrayList(properties);
    }

    public List<PropertyField> getResourcePropertyFields() {
        return propertyFields;
    }

    public Optional<PropertyField> findResourcePropertyField(String fieldName) {
        return propertyFields.stream().filter(field -> field.getName().equalsIgnoreCase(fieldName)).findFirst();
    }

    @SuppressWarnings("serial")
    public static List<String> getAllResourceTypeNames() {
        return Arrays.asList(ResourceType.values()).
                stream().
                map(resourceType -> resourceType.name().toLowerCase()).
                collect(Collectors.toList());
    }

    // case-insensitive version of enum.valueof
    public static ResourceType getResourceTypeFromName(String typeName) {
        for (ResourceType resourceType : ResourceType.values()) {
            if (resourceType.name().equalsIgnoreCase(typeName)) {
                return resourceType;
            }
        }

        throw new IllegalArgumentException("Unable to find resource type: " + typeName);
    }

    public static boolean resourceTypeWithNameExists(String resourceTypeName) {
        return ResourceType.getAllResourceTypeNames().contains(resourceTypeName.toLowerCase());
    }

    public Set<PropertyField> getProperties() {
        return this.propertyFields.stream().
                filter(field -> field.getType() != SECRET && field.getType() != FILE).
                collect(toSet());
    }

    public Set<PropertyField> getFieldsBy(Type type) {
        return this.propertyFields.
                stream().
                filter(field -> field.getType().equals(type)).
                collect(toSet());
    }

    /**
     * @deprecated 28.06.2016 Remove when old /conf api is gone
     * */
    @Deprecated
    public static List<String> getMandatoryFieldsFor(ResourceType resourceType) {
        return resourceType.getResourcePropertyFields().
                stream().
                filter(field -> !field.isOptional()).
                map(PropertyField::getName).
                collect(toList());
    }

    public static List<String> getAllFieldsFor(ResourceType resourceType) {
        return resourceType.getResourcePropertyFields().
                stream().
                map(PropertyField::getName).collect(toList());
    }

    /**
     * Typer som bor på en enkelt server
     */
    public static Set<ResourceType> serverDependentResourceTypes = ImmutableSet.of(DeploymentManager, OpenAm);

    /**
     * Typer som kan bli eksponert fra en applikasjon, men som "lever" på ett annet sted
     */
    public static Set<ResourceType> externalExposedResourceTypes = ImmutableSet.of(Queue);

    public ResourceTypeDocumentation getResourceDocumentation() {
        return documentation;
    }

    public static Comparator<ResourceType> comparator() {
        return Comparator.comparing(Enum::name);
    }
}
