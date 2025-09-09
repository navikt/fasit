package no.nav.aura.envconfig.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class ExceptionUtil {

    private ExceptionUtil() {
    }

    public static RuntimeException unpackInvocationException(InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException exception) {
            return exception;
        }
        return new RuntimeException(cause);
    }

    public static List<String> getMessages(final Throwable startException) {
        List<String> messages = new ArrayList<>();
        Set<Throwable> visited = new HashSet<>();
        Throwable exception = startException;
        while (exception != null && !visited.contains(exception)) {
            String message = exception.getMessage();
            messages.add(exception.getClass().getName() + ": " + Optional.ofNullable(message).orElse("(null)"));
            exception = exception.getCause();
        }
        return messages;
    }
}
