package no.nav.aura.envconfig.spring;

import static no.nav.aura.envconfig.util.ExceptionUtil.unpackInvocationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.function.Function;

import no.nav.aura.envconfig.util.ArrayHelper;
import no.nav.aura.envconfig.util.SerializableFunction;


public abstract class SecurityByPass {

    private static ThreadLocal<Integer> byPassEnabled = new ThreadLocal<>();

    private SecurityByPass() {
    }

    public static <T> T byPass(Function<Void, T> function) {
        try {
            modifyByPassValue(1);
            return function.apply(null);
        } finally {
            if (byPassEnabled.get() <= 1) {
                byPassEnabled.remove();
            } else {
                modifyByPassValue(-1);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrapWithByPass(Class<T> tClass, final T t) {
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), ArrayHelper.of(tClass), new InvocationHandler() {
            @SuppressWarnings("serial")
            public Object invoke(final Object proxy, final Method method, final Object[] args) {
                return byPass(new SerializableFunction<Void, T>() {
                    public T process(Void input) {
                        try {
                            return (T) method.invoke(t, args);
                        } catch (IllegalAccessException | IllegalArgumentException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw unpackInvocationException(e);
                        }
                    }
                });
            }
        });
    }

    private static void modifyByPassValue(int increment) {
        byPassEnabled.set(Optional.ofNullable(byPassEnabled.get()).orElse(0) + increment);
    }

    public static boolean isByPassEnabled() {
        return byPassEnabled.get() != null;
    }
}
