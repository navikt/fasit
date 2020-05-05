package no.nav.aura.envconfig.model.secrets;

import static no.nav.aura.envconfig.util.BytesHelper.cloneOrNull;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Lob;

import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import no.nav.aura.envconfig.model.AccessControl;
import no.nav.aura.envconfig.model.AccessControlled;
import no.nav.aura.envconfig.model.ModelEntity;
import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;
import no.nav.aura.envconfig.model.secrets.VaultClient;

import org.hibernate.envers.Audited;

@SuppressWarnings("serial")
@Entity
@Audited
public class Secret extends ModelEntity implements AccessControlled {
    @Lob
    private byte[] content;
    @Lob
    private byte[] iv;
    @Lob
    private byte[] keyId;
    @Column(name = "vault_path")
    private String vaultPath;

    @Embedded
    private AccessControl accessControl;

    protected Secret() {
    }

    public static Secret withValueAndAuthLevel(String clearText, EnvironmentClass authLevel) {
        Secret secret = new Secret();
        secret.setClearTextString(clearText, authLevel);
        secret.accessControl = new AccessControl(authLevel);
        return secret;
    }

    public static Secret withVaultPathAndAuthLevel(String vaultPath, EnvironmentClass authLevel) {
        Secret secret = new Secret();
        secret.vaultPath = vaultPath;
        secret.accessControl = new AccessControl(authLevel);
        return secret;
    }

    private Secret(String clearText, EnvironmentClass authLevel) {
        setClearTextString(clearText, authLevel);
        this.accessControl = new AccessControl(authLevel);
    }

    public Secret(Secret other) {
        this.content = other.content;
        this.iv = other.iv;
        this.keyId = other.keyId;
        this.accessControl = other.getAccessControl();
    }

    @Override
    public AccessControl getAccessControl() {
        return accessControl;
    }

    public byte[] getContent() {
        return cloneOrNull(content);
    }

    private byte[] getClearTextBytes() {
        if (!isInitialized()) {
            throw new RuntimeException("This Secret object has not been initialised yet");
        }
        KeyFactory kf = KeyFactory.getInstance();
        Key key = kf.getKeyForKeyId(new BigInteger(keyId));

        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm() + "/CBC/PKCS5Padding");
            IvParameterSpec ips = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ips);
            byte[] decoded = cipher.doFinal(content);
            return decoded;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isInitialized() {
        return keyId != null && this.content != null && this.iv != null;
    }

    public String getClearTextString() {
        if (vaultPath != null) {
            return getClearTextFromVault(vaultPath);
        } else {
            byte[] decoded = getClearTextBytes();
            try {
                return new String(decoded, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String getClearTextFromVault(String vaultPath) {
        int splitAt = vaultPath.lastIndexOf("/");
        String basepath = vaultPath.substring(0, splitAt);
        String field = vaultPath.substring(splitAt + 1);
        try {
            LogicalResponse response = VaultClient.getInstance().logical().read(basepath);

            Map<String, String> data = getSecretData(response);
            if (data.containsKey(field)) {
                return data.get(field);
            } else {
                throw new RuntimeException("The secret in Vault (" + basepath + ") did not contain field '" + field + "'");
            }
        } catch (VaultException ex) {
            throw new RuntimeException("Could not read secret from Vault at path " + basepath + " (HTTP status " + ex.getHttpStatusCode() + ": " + ex.getMessage() + ")");
        }
    }

    // Heuristic to detect if a secret comes from k/v store version 1 or 2. (The API response is a bit different)
    private static Map<String, String> getSecretData(LogicalResponse response) {
        List<String> keys = response.getDataObject().names();
        if (keys.size() == 2 && keys.contains("data") && keys.contains("metadata")) {
            Map<String, String> data = new HashMap<>();
            response.getDataObject().get("data").asObject().forEach(member -> {
                data.put(member.getName(), member.getValue().asString());
            });
            return data;
        } else {
            return response.getData();
        }
    }

    private void setClearTextBytes(byte[] clearText, EnvironmentClass authLevel) {
        KeyFactory kf = KeyFactory.getInstance();
        Key key;
        if (authLevel == null) {
            if (keyId == null) {
                throw new RuntimeException("This Secret object has not been initialized yet");
            }
            key = kf.getKeyForKeyId(new BigInteger(keyId));
        } else {
            key = kf.getKeyForEnvironmentClass(authLevel);
            keyId = KeyFactory.getKeyId(key).toByteArray();
        }

        try {
            Cipher cipher = Cipher.getInstance(key.getAlgorithm() + "/CBC/PKCS5Padding");
            byte[] newIv = new byte[cipher.getBlockSize()];
            new SecureRandom().nextBytes(newIv);
            IvParameterSpec ips = new IvParameterSpec(newIv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ips);
            byte[] encoded = cipher.doFinal(clearText);
            content = encoded;
            iv = newIv;
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key - you might need to modify the JRE security policy to allow key lengths larger than 128 bits", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final void setClearTextString(String clearText, EnvironmentClass authLevel) {
        if (clearText == null) {
            clearText = "";
        }
        try {
            byte[] clearTextBytes = clearText.getBytes("UTF-8");
            setClearTextBytes(clearTextBytes, authLevel);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getIV() {
        return cloneOrNull(iv);
    }

    public byte[] getKeyId() {
        return cloneOrNull(keyId);
    }

    @Override
    public String getName() {
        return "NA";
    }

    public String getVaultPath() {
        return vaultPath;
    }

    protected void setContent(byte[] content) {
        this.content = content;
    }

    protected void setIv(byte[] iv) {
        this.iv = iv;
    }

    protected void setKeyId(byte[] keyId) {
        this.keyId = keyId;
    }
    
    @Override
    public String toString() {
        return "secret id: " + getID();
    }

}
