package no.nav.aura.appconfig;

import java.util.Collection;
import java.util.HashSet;

/**
 * Not using guava because of dependency aversion
 */
@SuppressWarnings("unchecked")
public class Filter {
    public static <T> Collection<T> filter(Collection<?> collection, Class<T> type) {
        HashSet<T> filtered = new HashSet<>();
        for (Object o : collection) {
            if (type.isInstance(o)) {
                filtered.add((T) o);
            }
        }
        return filtered;
    }
}