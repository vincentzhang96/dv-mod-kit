package com.divinitor.dn.lib.game.mod.compiler.processors;

import co.phoenixlab.dn.subfile.stage.propinfo.PropEntry;
import co.phoenixlab.dn.subfile.stage.propinfo.PropInfo;
import co.phoenixlab.dn.subfile.stage.propinfo.PropInfoReader;
import co.phoenixlab.dn.util.LittleEndianDataOutputStream;
import com.divinitor.dn.lib.game.mod.compiler.processors.propinfo.PropInfoDescriptor;
import com.divinitor.dn.lib.game.mod.compiler.processors.propinfo.PropInfoModification;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PropInfoProcessor implements Processor {
    @Override
    public Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src) {
        return () -> {
            Gson gson = new GsonBuilder()
                .create();
            byte[] descriptorData = modPack.getAsset(src);
            PropInfoDescriptor desc = gson.fromJson(
                new InputStreamReader(new ByteArrayInputStream(descriptorData)),
                PropInfoDescriptor.class);
            byte[] result = apply(modPack, desc);

            try  {
                Path root = modPack.getKit().getRoot();
                Path temp = root.resolve("temp");
                Files.createDirectories(temp);
                String tempFileName = modPack.getId() + "-" + src + "-propinfo.ini";
                Path tempFile = temp.resolve(tempFileName);
                Files.deleteIfExists(tempFile);
            } catch (Exception e) {
            }

            return result;
        };
    }

    private byte[] apply(ModPackage modPack,  PropInfoDescriptor desc) throws Exception {
        byte[] propInfo = modPack.getKit().getAssetAccessService().getAsset("/mapdata/grid/" + desc.getMapName() + "/0_0/propinfo.ini");
        ByteBuffer buf = ByteBuffer.wrap(propInfo);
        PropInfoReader reader = new PropInfoReader();
        PropInfo info = reader.read(buf);

        int written = 0;
        ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
        for (PropEntry prop : info.getProps()) {
            String sknFile = prop.getSknFile().toLowerCase();
            PropInfoModification matchingMod = desc.getMods().stream()
                .filter((mod) -> {
                    String sknName = mod.getSknName();
                    if (sknName != null) {
                        sknName = sknName.toLowerCase();
                        if (sknName.equals(sknFile) || sknFile.contains(sknName) || sknFile.matches(sknName)) {
                            return true;
                        }
                    }

                    return mod.getPropId() == prop.getPropId();
                })
                .findAny()
                .orElse(null);
            if (matchingMod != null) {
                if (matchingMod.getAction() == PropInfoModification.Action.DELETE) {
                    continue;
                } else if (matchingMod.getAction() == PropInfoModification.Action.MODIFY) {
                    // TODO
                }
            }

            bodyOut.write(propInfo, prop.getOffset(), prop.getEntrySize() + 4);
            ++written;
        }

        ByteArrayOutputStream totalOut = new ByteArrayOutputStream();
        LittleEndianDataOutputStream tOut = new LittleEndianDataOutputStream(totalOut);
        tOut.writeInt(info.getUnknown1());
        tOut.writeInt(written);
        tOut.write(bodyOut.toByteArray());
        return totalOut.toByteArray();
    }
}
