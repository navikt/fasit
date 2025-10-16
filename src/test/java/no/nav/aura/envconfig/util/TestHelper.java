package no.nav.aura.envconfig.util;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;

public abstract class TestHelper {

    private TestHelper() {
    }

    public static <T> T assertAndGetSingle(Collection<T> collection) {
        assertThat(collection.size(), is(1));
        return collection.iterator().next();
    }

    public static <T> T assertAndGetSingleOrNull(Collection<T> collection) {
        assertThat(collection.size(), oneOf(0, 1));
        return collection.stream().findFirst().orElse(null);
    }

    public static void setUpEncryption() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
    }

}
