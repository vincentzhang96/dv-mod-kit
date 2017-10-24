package com.divinitor.dn.lib.game.mod.compiler;

import co.phoenixlab.dn.subfile.dnt.Dnt;
import co.phoenixlab.dn.subfile.dnt.DntReader;
import co.phoenixlab.dn.util.LittleEndianDataOutputStream;
import com.divinitor.dn.lib.game.mod.DnAssetAccessService;
import com.divinitor.dn.lib.game.mod.definition.TableEditDirective;
import com.divinitor.dn.lib.game.mod.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TableEditor {

    private final DnAssetAccessService assetAccessService;

    public TableEditor(DnAssetAccessService assetAccessService) {
        this.assetAccessService = assetAccessService;
    }

    public Utils.ThrowingSupplier<byte[]> tableEdit(
        String tableName,
        TableEditDirective directive) {
        return () -> this.compileTable(tableName, directive);
    }

    public byte[] compileTable(String tableName, TableEditDirective directive) throws IOException {
        byte[] tableBytes = this.assetAccessService.getAsset(tableName);
        DntReader reader = new DntReader();
        DntReader.DntHandle handle = reader.read(ByteBuffer.wrap(tableBytes));
        Dnt dnt = handle.getDnt();

        BodyResult body = this.buildBody(handle, directive);


        ByteArrayOutputStream out = new ByteArrayOutputStream(tableBytes.length);
        LittleEndianDataOutputStream dout = new LittleEndianDataOutputStream(out);
        dout.writeInt(dnt.getMagicNumber());
        dout.writeShort(dnt.getNumColumns());
        dout.writeInt(body.numRows);
        dout

        return out.toByteArray();
    }

    private BodyResult buildBody(DntReader.DntHandle handle, TableEditDirective directive) {

        return new BodyResult();
    }

    static class BodyResult {
        byte[] data;
        int numRows;
    }
}
