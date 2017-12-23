package com.divinitor.dn.lib.game.mod.compiler.processors;

import co.phoenixlab.dn.subfile.act.kit.ActKitCompiler;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;

public class ActKitProcessor implements Processor {
    @Override
    public Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src) {
        return () -> {
            //  src is a real relative path relative to the mod base dir
            Path baseDir = modPack.getKit().resolveModPackagePath(modPack, src);
            ActKitCompiler compiler = new ActKitCompiler();
            Path root = modPack.getKit().getRoot();
            Path temp = root.resolve("temp");
            Files.createDirectories(temp);
            String tempFileName = baseDir.getFileName().toString() + ".act";
            Path tempFile = temp.resolve(tempFileName);
            Files.deleteIfExists(tempFile);
            compiler.compile(baseDir, tempFile);
            return Files.readAllBytes(tempFile);
        };
    }
}
