package com.divinitor.dn.lib.game.mod.util;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class VersionGsonAdapter extends TypeAdapter<Version> {
    /**
     * Writes one JSON value (an array, object, string, number, boolean or null)
     * for {@code value}.
     *
     * @param out
     * @param value the Java object to write. May be null.
     */
    @Override
    public void write(JsonWriter out, Version value) throws IOException {
        out.value(value.toString());
    }

    /**
     * Reads one JSON value (an array, object, string, number, boolean or null)
     * and converts it to a Java object. Returns the converted object.
     *
     * @param in
     * @return the converted Java object. May be null.
     */
    @Override
    public Version read(JsonReader in) throws IOException {
        return Version.valueOf(in.nextString());
    }
}
