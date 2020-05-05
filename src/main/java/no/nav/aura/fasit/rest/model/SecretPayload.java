package no.nav.aura.fasit.rest.model;

import java.net.URI;

import no.nav.aura.fasit.rest.SecretRest;

public class SecretPayload {
    public String value;
    public String vaultpath;
    public URI ref;
    
    public SecretPayload() {
    }

    public static SecretPayload withValue(String value) {
        return new SecretPayload(value, null);
    }

    public static SecretPayload withVaultPath(String vaultPath) {
        return new SecretPayload(null, vaultPath);
    }

    public static SecretPayload withURI(URI ref) {
        return new SecretPayload(ref);
    }

    public static SecretPayload withIdAndBaseUri(long id, URI baseUri) {
        return new SecretPayload(id, baseUri);
    }
    
    private SecretPayload(String value, String vaultpath) {
        if (value != null ^ vaultpath != null) {
            this.value = value;
            this.vaultpath = vaultpath;
        } else {
            throw new IllegalArgumentException("Either value or vaultpath must be set (but not both).");
        }
    }

    private SecretPayload(URI ref) {
        this.ref = ref;
    }

    private SecretPayload(long id, URI baseUri) {
        this.ref = SecretRest.secretUri(baseUri, id);
    }
}
