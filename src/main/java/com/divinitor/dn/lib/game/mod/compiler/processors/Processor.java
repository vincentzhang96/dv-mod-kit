package com.divinitor.dn.lib.game.mod.compiler.processors;

import com.divinitor.dn.lib.game.mod.definition.ModPackage;
import com.divinitor.dn.lib.game.mod.util.Utils;

public interface Processor {

    Utils.ThrowingSupplier<byte[]> process(ModPackage modPack, String src);

}
