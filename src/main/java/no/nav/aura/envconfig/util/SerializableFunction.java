package no.nav.aura.envconfig.util;

import java.io.Serializable;

import com.google.common.base.Function;

@SuppressWarnings("serial")
public abstract class SerializableFunction<F, T> implements Function<F, T>, Serializable {

    /** Overriding nullable method to avoid trouble with checkstyle */
    public final T apply(F input) {
        return process(input);
    }

    public abstract T process(F input);
}
