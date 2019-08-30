package com.divinitor.dn.lib.game.mod.compiler.processors;

import co.phoenixlab.dn.subfile.stage.triggerdefine.TriggerDefine;
import com.divinitor.dn.lib.game.mod.compiler.processors.models.StageTriggerModels;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class StageTriggerV2Processor implements Processor {
    @Override
    public Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src) {
        return () -> {
            Gson gson = new GsonBuilder()
//                .registerTypeAdapter(TriggerScriptCall.class, new TriggerScriptCallConverter())
                .create();
            byte[] data = modPack.getAsset(src);
            StageTriggerModels.TriggerScript triggers = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(data)), StageTriggerModels.TriggerScript.class);

            Map.Entry<byte[], TriggerDefine> written = StageTriggerModels.writeTriggerVariableDefinitions(triggers);

            byte[] result = StageTriggerModels.writeTriggers(triggers, written.getValue().getEntries());

            try  {
                Path root = modPack.getKit().getRoot();
                Path temp = root.resolve("temp");
                Files.createDirectories(temp);
                String tempFileName = modPack.getId() + "-" + src + "trigger.ini";
                Path tempFile = temp.resolve(tempFileName);
                Files.write(tempFile, result);
            } catch (Exception e) {
            }

            return result;
        };
    }
}
