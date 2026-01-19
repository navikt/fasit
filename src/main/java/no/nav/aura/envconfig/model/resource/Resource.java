package no.nav.aura.envconfig.model.resource;

import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.Scopeable;
import no.nav.aura.envconfig.model.deletion.DeleteableEntity;
import no.nav.aura.envconfig.model.secrets.Secret;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("serial")
@Entity
@Audited
@Table(name = "resource_table")
public class Resource extends DeleteableEntity implements Scopeable, AccessControlled {

    private enum PropertyFieldCategory {
        PROPERTY, SECRET, FILE
    };

    @Column(name = "resource_alias")
    private String alias;
    @Embedded
    private Scope scope;

    @ElementCollection
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value")
    @CollectionTable(name = "resource_properties")
    private Map<String, String> properties = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType type;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "resource_secrets", joinColumns = { @JoinColumn(name="resource_table_entid") })
    @MapKeyColumn(name = "secret_key")
    @AuditJoinTable(name = "resource_secrets_aud")
    private Map<String, Secret> secrets = new HashMap<>();

    @ManyToMany(cascade = {CascadeType.ALL})
    @JoinTable(name = "resource_files", joinColumns = { @JoinColumn(name="resource_table_entid") })
    @MapKeyColumn(name = "file_key")
    @NotAudited
    private Map<String, FileEntity> fileEntities = new HashMap<>();
    private boolean dodgy;

    @Embedded
    private AccessControl accessControl;

    Resource() {
    }

    public Resource(String alias, ResourceType type, Scope scope) {
        this.alias = alias;
        this.scope = scope;
        this.type = type;
        setupPropertyMap(type);
        this.accessControl = new AccessControl(scope.getEnvClass());
    }

    public Resource(Resource other) {
        this(other.alias, other.type, other.scope);
        this.properties = new HashMap<>(other.properties); 
        this.secrets = other.secrets.entrySet().stream()
			.collect(Collectors.toMap(
					Map.Entry::getKey, entry -> {
				          assert entry.getKey() != null && entry.getValue()!= null;
				            return new Secret(entry.getValue());
					}
			));
        		
        this.fileEntities = other.fileEntities.entrySet().stream()
        		.collect(Collectors.toMap(
        				Map.Entry::getKey, entry -> {
							assert entry.getKey() != null && entry.getValue() != null;
							return new FileEntity(entry.getValue());
						}
				));
    }

    /**
     * @return a environment independent name of the resource (will be referenced in app-config.xml). The actual resource
     * information will be resolved using the context.
     */
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

     @Override
     public String getInfo() {
         return type + " | " + scope.asDisplayStringWithZone();
     }

    @Override
    public Map<String, Object> getEnityProperties() {
        Map<String, Object> resourceDetails = new HashMap();

        resourceDetails.put("type", type);
        resourceDetails.put("scope", scope.asDisplayStringWithZone());

        for (String key : this.properties.keySet()) {
            resourceDetails.put(key, properties.get(key));
        }

        return resourceDetails;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
        this.accessControl.setEnvironmentClass(scope.getEnvClass());
    }

    @Override
    protected HashCodeBuilder createHashCodeBuilder() {
        return super.createHashCodeBuilder().append(alias).append(type).append(scope);
    }

    @Override
    protected EqualsBuilder createEqualsBuilder(ModelEntity obj) {
        Resource other = (Resource) obj;
        return super.createEqualsBuilder(other).append(alias, other.alias).append(type, other.type).append(scope, other.scope);
    }

    @Override
    public String getName() {
        return alias;
    }


    private void setupPropertyMap(ResourceType type) {
        for (PropertyField propertyField : type.getResourcePropertyFields()) {
            String key = propertyField.getName();
            switch (getFieldCategory(propertyField)) {
                case PROPERTY:
                    if (!properties.containsKey(key)) {
                        properties.put(key, null);
                    }
                    break;
                case SECRET:
                    if (!secrets.containsKey(key)) {
                        secrets.put(key, Secret.withValueAndAuthLevel(null, getScope().getEnvClass()));
                    }
                    break;
                case FILE:
                    if (!fileEntities.containsKey(key)) {
                        fileEntities.put(key, new FileEntity());
                    }
                    break;
            }
        }
    }

    public Map<String, String> getProperties() {
        setupPropertyMap(type);
        return properties;
    }

    public ResourceType getType() {
        return type;
    }

    public Map<String, Secret> getSecrets() {
        setupPropertyMap(type);
        return Map.copyOf(secrets);
    }

    public Map<String, FileEntity> getFiles() {
        setupPropertyMap(type);
        return fileEntities;
    }

    @Deprecated
    /** 04.07-16 @mats Remove when old api is deprecated.
     * Need to be here until then to preserve backward compatability since old api is case sensitive on property keys and new api is not
     * */
    public void putPropertyAndValidate(String key, String value) {
        checkIfKeyIsValid(key, PropertyFieldCategory.PROPERTY);
        properties.put(key, value);
    }

    @Deprecated
    /** 04.07-16 @mats Remove when old api is deprecated.
     * Need to be here until then to preserve backward compatability since old api is case sensitive on property keys and new api is not
     * */
    public void putSecretAndValidate(String key, String secretClearText) {
        checkIfKeyIsValid(key, PropertyFieldCategory.SECRET);
        Secret oldSecret = secrets.get(key);

        if (!oldSecret.getClearTextString().equals(secretClearText)) {
            Secret newSecret = Secret.withValueAndAuthLevel(secretClearText, getScope().getEnvClass());
            secrets.put(key, newSecret);
        }
    }

    @Deprecated
    /** 04.07-16 @mats Remove when old api is deprecated.
     * Need to be here until then to preserve backward compatability since old api is case sensitive on property keys and new api is not
     * */
    public void putFileAndValidate(String key, FileEntity file) {
        checkIfKeyIsValid(key, PropertyFieldCategory.FILE);
        fileEntities.put(key, file);
    }

    public void putProperty(String key, String value) {
        properties.put(key, value);
    }

    public void putSecretWithValue(String key, String secretClearText) {
        Secret oldSecret = secrets.get(key);

        if (!oldSecret.getClearTextString().equals(secretClearText)) {
            Secret newSecret = Secret.withValueAndAuthLevel(secretClearText, getScope().getEnvClass());
            secrets.put(key, newSecret);
        }
    }

    public void putSecretWithVaultPath(String key, String vaultPath) {
        Secret oldSecret = secrets.get(key);
        if (!StringUtils.equals(oldSecret.getVaultPath(), vaultPath)) {
            Secret newSecret = Secret.withVaultPathAndAuthLevel(vaultPath, getScope().getEnvClass());
            secrets.put(key, newSecret);
        }
    }

    public void putFile(String key, FileEntity file) {
        fileEntities.put(key, file);
    }


    /***
     * Deprecated 1.7 2016 Removed this when old /conf resource api is fased out
     */
    @Deprecated
    private void checkIfKeyIsValid(String key, PropertyFieldCategory expectedCategory) {
        List<PropertyField> validProperties = type.getResourcePropertyFields();
        for (PropertyField propertyField : validProperties) {
            PropertyFieldCategory fieldCategory = getFieldCategory(propertyField);
            if (propertyField.getName().equals(key) && fieldCategory.equals(expectedCategory)) {
                return;
            }
        }
        List<String> validPropertyNames = validProperties.stream().map(input -> input.getName()).collect(toList());
        throw new IllegalArgumentException("Property with name " + key + " is invalid for resource " + type + " valid names is " + validPropertyNames);

    }

    private PropertyFieldCategory getFieldCategory(PropertyField field) {
        switch (field.getType()) {
            case ENUM:
            case TEXT:
                return PropertyFieldCategory.PROPERTY;
            case SECRET:
                return PropertyFieldCategory.SECRET;
            case FILE:
                return PropertyFieldCategory.FILE;
        }
        throw new IllegalArgumentException("Property of type " + field.getType() + " is unknown");

    }

    public String getHostName() {
        if (!isServerDependent()) {
            throw new IllegalArgumentException(getType() + " does not have a hostname");
        }
        return properties.get("hostname");
    }

    public boolean isServerDependent() {
        return ResourceType.serverDependentResourceTypes.contains(getType());
    }

    @Override
    public int hashCode() {
        return createHashCodeBuilder().build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Resource)) {
            return false;
        }
        Resource other = (Resource) obj;
        return createEqualsBuilder(other).append(type, other.type).build();
    }

    public void markAsDodgy(boolean dodgy) {
        this.dodgy = dodgy;
    }

    public boolean isDodgy() {
        return dodgy;
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }
}
