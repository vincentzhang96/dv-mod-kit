package com.divinitor.dn.lib.game.mod.util;

public class VersionCached<T> {

    private Versioned versioned;

    private int cachedVersion;

    private T cachedObject;

    public VersionCached(Versioned versioned) {
        this.versioned = versioned;
    }

    public int getVersion() {
        return cachedVersion;
    }

    public T get() {
        return cachedObject;
    }

    public boolean isValid() {
        return versioned.getVersion() == cachedVersion && cachedObject != null;
    }

    public void set(T t) {
        cachedObject = t;
        cachedVersion = versioned.getVersion();
    }

    public interface Versioned {
        int getVersion();
    }
}
