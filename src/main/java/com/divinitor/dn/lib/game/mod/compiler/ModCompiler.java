package com.divinitor.dn.lib.game.mod.compiler;

import com.divinitor.dn.lib.game.mod.CompileException;
import com.divinitor.dn.lib.game.mod.DnAssetAccessService;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface ModCompiler {
    BuildComputeResults compute();

    void compile() throws CompileException;



    static Utils.ThrowingSupplier<byte[]> gameSource(DnAssetAccessService assetAccessService, String file) {
        return () -> assetAccessService.getAsset(file);
    }

    static Utils.ThrowingSupplier<byte[]> packSource(ModPackage modPackage, String file) {
        return () -> modPackage.getAsset(file);
    }

    @Getter
    @AllArgsConstructor
    class FileBuildStep {
        private ModPackage mod;
        private String destination;
        private Utils.ThrowingSupplier<byte[]> source;
        private Integer compressionLevel;
    }
}
