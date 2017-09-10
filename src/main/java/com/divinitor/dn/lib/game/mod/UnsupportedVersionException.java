package com.divinitor.dn.lib.game.mod;

import com.github.zafarkhaja.semver.Version;

public class UnsupportedVersionException extends RuntimeException {

    public UnsupportedVersionException() {
    }

    public UnsupportedVersionException(String message) {
        super(message);
    }

    public UnsupportedVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedVersionException(Throwable cause) {
        super(cause);
    }

    public UnsupportedVersionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UnsupportedVersionException(Version expected, Version actual) {
        super("Version " + actual.toString() + " is incompatible with " + expected.toString());
    }
}
