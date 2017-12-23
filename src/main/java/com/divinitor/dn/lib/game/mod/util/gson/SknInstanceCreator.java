package com.divinitor.dn.lib.game.mod.util.gson;

import co.phoenixlab.dn.subfile.skn.Skn;
import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

public class SknInstanceCreator implements InstanceCreator<Skn> {
    @Override
    public Skn createInstance(Type type) {
        return new Skn();
    }
}
