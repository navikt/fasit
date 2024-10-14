package no.nav.aura.envconfig.model.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LookupResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;

public final class VaultClient {
    private final static Logger log = LoggerFactory.getLogger(VaultClient.class);

    private static Vault instance;
    private static final Timer timer = new Timer("VaultScheduler", true);;
    private static final int MIN_REFRESH_MARGIN = 10 * 60 * 1000; // 10 min in ms;

    private VaultClient() {
    }

    public static Vault getInstance() {
        if (instance == null) {
            // Verify that the token is ok
            LookupResponse response;

            try {
                instance = new Vault(new VaultConfig()
                        .address(System.getProperty("vault.url"))
                        .engineVersion(1)
                        .token(getVaultToken())
                        .build());
                response = instance.auth().lookupSelf();
                log.info("Logged in to Vault as: " + response.getDisplayName());
            } catch (Exception e) {
                throw new RuntimeException("Could not initialise the Vault client", e);
            }

            if (response.isRenewable()) {
                final class RefreshTokenTask extends TimerTask {
                    @Override
                    public void run() {
                        try {
                            log.info("Refreshing Vault token (old TTL = " + instance.auth().lookupSelf().getTTL() + " seconds)");
                            AuthResponse response = instance.auth().renewSelf();
                            log.info("Refreshed Vault token (new TTL = " + instance.auth().lookupSelf().getTTL() + " seconds)");
                            timer.schedule(new RefreshTokenTask(), suggestedRefreshInterval(response.getAuthLeaseDuration() * 1000));
                        } catch (VaultException e) {
                            log.error("Could not refresh the Vault token", e);

                            // Lets try refreshing again
                            log.warn("Waiting 5 secs before trying to refresh the Vault token");
                            timer.schedule(new RefreshTokenTask(), 5000);
                        }
                    }
                }

                log.info("Starting a refresh timer on the vault token (TTL = " + response.getTTL() + " seconds");
                timer.schedule(new RefreshTokenTask(), suggestedRefreshInterval(response.getTTL() * 1000));
            } else {
                log.warn("Vault token is not renewable");
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

    // We should refresh tokens from Vault before they expire, so we add a MIN_REFRESH_MARGIN margin.
    // If the token is valid for less than MIN_REFRESH_MARGIN * 2, we use duration / 2 instead.
    private static long suggestedRefreshInterval(long duration) {
        if (duration < MIN_REFRESH_MARGIN * 2) {
            return duration / 2;
        } else {
            return duration - MIN_REFRESH_MARGIN;
        }
    }
}
