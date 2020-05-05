package no.nav.aura.envconfig.util;

import java.io.Serializable;

import com.google.common.base.Predicate;

@SuppressWarnings("serial")
public abstract class SerializablePredicate<T> implements Predicate<T>, Serializable {

    public final boolean apply(T input) {
        return test(input);
    }

    public abstract boolean test(T t);

}
