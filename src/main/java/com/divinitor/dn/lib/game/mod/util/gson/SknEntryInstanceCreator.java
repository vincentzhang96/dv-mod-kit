package com.divinitor.dn.lib.game.mod.util.gson;

import co.phoenixlab.dn.subfile.skn.SknEntry;
import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

public class SknEntryInstanceCreator implements InstanceCreator<SknEntry> {
    @Override
    public SknEntry createInstance(Type type) {
        return new SknEntry();
    }
}
