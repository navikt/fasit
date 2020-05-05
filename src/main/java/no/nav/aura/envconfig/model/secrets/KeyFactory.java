package no.nav.aura.envconfig.model.secrets;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import no.nav.aura.envconfig.model.infrastructure.EnvironmentClass;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class KeyFactory {

    private static final Logger log = LoggerFactory.getLogger(KeyFactory.class);

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    private final KeyStore keyStore;
    private final Map<BigInteger, String> keys = new HashMap<BigInteger, String>();
    private final Map<EnvironmentClass, BigInteger> defaultKeyAliases = new HashMap<EnvironmentClass, BigInteger>();

    private static KeyFactory instance;

    private final char[] keyStorePassword;

    private final String keyStoreFileName;

    static KeyFactory getInstance() {
        synchronized (KeyFactory.class) {
            if (instance == null) {
                instance = new KeyFactory();
            }
        }
        return instance;
    }

    KeyFactory() {
        log.info("This is in the constructor for KeyFactory - preparing encryption/decryption.");

        String credentialName = System.getProperty("fasit.encryptionkeys.username");
        if (credentialName == null) {
            throw new IllegalArgumentException("No credential with property fasit.encryptionkeys is defined. Can not select encryption keys");
        }

        try {
            log.info("Getting jceks instance of KeyStore");
            keyStore = KeyStore.getInstance("jceks");
            keyStorePassword = System.getProperty("fasit.encryptionkeys.password").toCharArray();
            log.info("Found password, " + keyStorePassword.length + " characters.");

            String keyStorePath = System.getProperty("fasit.encryptionkeys.path");
            if (keyStorePath != null) {
                log.info("Using keystore from file system path {}", keyStorePath);
                keyStoreFileName = keyStorePath;
                keyStore.load(new FileInputStream(keyStorePath), keyStorePassword);
            } else {
                keyStoreFileName = "/keystore/keystore_" + credentialName + ".jceks";
                log.info("Using keystore from classpath {}", keyStoreFileName);
                URL keyStoreFile = getClass().getResource(keyStoreFileName);
                if (keyStoreFile == null) {
                    throw new IllegalArgumentException("No keystore with name " + keyStoreFileName + " found on classpath");
                }
                keyStore.load(getClass().getResourceAsStream(keyStoreFileName), keyStorePassword);
            }

            populateKeyMaps();

            int maxSize = Cipher.getMaxAllowedKeyLength(ALGORITHM);
            if (KEY_SIZE > maxSize) {
                log.warn("The JRE in use restricts the key length to a maximum of " + maxSize + " bits. "
                        + "If the key store in use already contains longer keys, trying to use these will "
                        + "cause Illegal Key Length exceptions. You must install"
                        + "unrestricted key length policy files in the JRE to lift this restriction.");
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    Key getKeyForKeyId(BigInteger keyId) {
        if (keys.containsKey(keyId)) {
            try {
                return keyStore.getKey(keys.get(keyId), this.keyStorePassword);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        } else {
            String keyIdFormatted = Hex.encodeHexString(keyId.toByteArray());
            throw new IllegalArgumentException("Key ID " + keyIdFormatted + " not found. Please ensure that the key store installed at " + keyStoreFileName + " contains this key.");
        }
    }

    Key getKeyForEnvironmentClass(EnvironmentClass envClass) {
        return getKeyForKeyId(defaultKeyAliases.get(envClass));
    }

    private void populateKeyMaps() throws GeneralSecurityException {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!keyStore.isKeyEntry(alias)) {
                continue;
            }
            Key key = keyStore.getKey(alias, this.keyStorePassword);
            BigInteger keyId = getKeyId(key);
            keys.put(keyId, alias);
            for (EnvironmentClass envClass : EnvironmentClass.values()) {
                if (envClass.name().equals(alias)) {
                    defaultKeyAliases.put(envClass, keyId);
                    break;
                }
            }
        }
    }

    public static BigInteger getKeyId(Key key) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] digest = sha1.digest(key.getEncoded());
        return new BigInteger(digest);
    }

}
