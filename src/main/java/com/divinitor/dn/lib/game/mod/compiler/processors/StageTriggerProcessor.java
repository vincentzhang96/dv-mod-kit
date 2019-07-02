package com.divinitor.dn.lib.game.mod.compiler.processors;

import co.phoenixlab.dn.subfile.stage.trigger.StageTriggers;
import co.phoenixlab.dn.subfile.stage.trigger.StageTriggersWriter;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class StageTriggerProcessor implements Processor {
    @Override
    public Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src) {
        return () -> {
            Gson gson = new GsonBuilder()
//                .registerTypeAdapter(TriggerScriptCall.class, new TriggerScriptCallConverter())
                .create();
            byte[] data = modPack.getAsset(src);
            StageTriggers triggers = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(data)), StageTriggers.class);
            StageTriggersWriter writer = new StageTriggersWriter();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writer.write(triggers, baos);
            byte[] result = baos.toByteArray();

            try  {
                Path root = modPack.getKit().getRoot();
                Path temp = root.resolve("temp");
                Files.createDirectories(temp);
                String tempFileName = modPack.getId() + "-" + src + "trigger.ini";
                Path tempFile = temp.resolve(tempFileName);
                Files.deleteIfExists(tempFile);
            } catch (Exception e) {
            }

            return result;
        };
    }
}
