package com.divinitor.dn.lib.game.mod.util;

import java.util.Objects;
import java.util.function.Consumer;

public class Utils {

    private Utils() {}

    public static RuntimeException sneakyThrow(Throwable t) {
        Objects.requireNonNull(t);
        return sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        assert t != null;
        throw (T) t;
    }

    public static <T> Consumer<T> sneakyConsumer(ThrowingConsumer<T> c) {
        return t -> {
            try {
                c.accept(t);
            } catch (Exception e) {
                throw sneakyThrow(e);
            }
        };
    }

    public static Runnable sneakyRunnable(ThrowingRunnable t) {
        return () -> {
            try {
                t.run();
            } catch (Exception e) {
                throw sneakyThrow(e);
            }
        };
    }

    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
