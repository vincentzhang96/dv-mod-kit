package com.divinitor.dn.lib.game.mod.compiler.processors;

import co.phoenixlab.dn.subfile.skn.Skn;
import co.phoenixlab.dn.subfile.skn.SknEntry;
import co.phoenixlab.dn.subfile.skn.SknWriter;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.Utils;
import com.divinitor.dn.lib.game.mod.util.gson.SknEntryInstanceCreator;
import com.divinitor.dn.lib.game.mod.util.gson.SknInstanceCreator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

public class SknProcessor implements Processor {
    @Override
    public Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src) {
        return () -> {
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(Skn.class, new SknInstanceCreator())
                .registerTypeAdapter(SknEntry.class, new SknEntryInstanceCreator())
                .create();
            byte[] data = modPack.getAsset(src);
            Skn skn = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(data)), Skn.class);
            SknWriter writer = new SknWriter();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writer.write(skn, baos);
            return baos.toByteArray();
        };
    }
}
