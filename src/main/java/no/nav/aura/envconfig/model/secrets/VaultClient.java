package no.nav.aura.envconfig.model.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.response.LookupResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class VaultClient {
    private final static Logger log = LoggerFactory.getLogger(VaultClient.class);

    private static Vault instance;
    private VaultClient() {
    }

    public static Vault getInstance() {
        if (instance == null) {
            try {
                instance = new Vault(new VaultConfig()
                        .address(System.getProperty("vault.url"))
                        .engineVersion(1)
                        .token(getVaultToken())
                        .build());
                LookupResponse response = instance.auth().lookupSelf();
                log.info("Logged in to Vault as: " + response.getDisplayName());
            } catch (Exception e) {
                throw new RuntimeException("Could not initialise the Vault client", e);
            }
        }
        return instance;
    }

    private static String getVaultToken() throws Exception {
        if (System.getProperty("vault.token") != null) {
            return System.getProperty("vault.token");
        }

        String tokenFromEnv = System.getenv("VAULT_TOKEN");
        if (!StringUtils.isEmpty(tokenFromEnv)) {
            log.info("Using VAULT_TOKEN environment variable.");
            return tokenFromEnv;
        }

        Path homedirVaultToken = Paths.get(System.getProperty("user.home"), ".vault-token");
        if (Files.exists(homedirVaultToken)) {
            return readVaultTokenFromFile(homedirVaultToken);
        }

        Path srvfasitVaultToken = Paths.get("/var/run/secrets/nais.io/vault/vault_token");
        if (Files.exists(srvfasitVaultToken)) {
            return readVaultTokenFromFile(srvfasitVaultToken);
        }

        throw new Exception("Could not find a Vault token to authenticate with!");
    }

    private static String readVaultTokenFromFile(Path path) throws Exception {
        log.info("Reading vault token from {}", path);
        return StringUtils.join(IOUtils.readLines(new FileInputStream(path.toFile())), "\n");
    }
}
