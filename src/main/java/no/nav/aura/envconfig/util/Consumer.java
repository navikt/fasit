package no.nav.aura.envconfig.util;

@SuppressWarnings("serial")
public abstract class Consumer<T> extends SerializableFunction<T, Void> {

    public abstract void perform(T t);

    public final Void process(T t) {
        perform(t);
        return null;
    }
}
