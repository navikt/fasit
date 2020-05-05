package no.nav.aura.envconfig.util;

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.collect.Iterables.tryFind;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
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
        assertThat(collection.size(), isOneOf(0, 1));
        return tryFind(collection, alwaysTrue()).orNull();
    }

    public static void setUpEncryption() {
        System.setProperty("fasit.encryptionkeys.username", "junit");
        System.setProperty("fasit.encryptionkeys.password", "password");
    }

}
