package no.nav.aura.envconfig.util;

import com.google.common.base.Supplier;

@SuppressWarnings("serial")
public abstract class Producer<T> extends SerializableFunction<Void, T> implements Supplier<T> {

    public final T process(Void input) {
        return get();
    }

    @Override
    public abstract T get();

}
