package com.divinitor.dn.lib.game.mod.constraints;

import java.util.Map;
import java.util.StringJoiner;

public class ConstraintViolationException extends RuntimeException {
    public ConstraintViolationException() {
    }

    public ConstraintViolationException(String message) {
        super(message);
    }

    public ConstraintViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintViolationException(Throwable cause) {
        super(cause);
    }

    public ConstraintViolationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static ConstraintViolationException of(Map<String, String> fields) {
        return of(fields, null);
    }

    public static ConstraintViolationException of(Map<String, String> fields, Throwable cause) {
        StringJoiner joiner = new StringJoiner("\n\t");
        fields.forEach((k, v) -> joiner.add(String.format("%-12s: %s", k, v)));
        return new ConstraintViolationException(joiner.toString(), cause);
    }

    public static void throwViolationException(Map<String, String> fields) throws ConstraintViolationException {
        if (!fields.isEmpty()) {
            throw of(fields);
        }
    }
}
