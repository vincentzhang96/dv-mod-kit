package com.divinitor.dn.lib.game.mod.compiler;

import co.phoenixlab.dn.subfile.dnt.Dnt;
import co.phoenixlab.dn.subfile.dnt.DntColumn;
import co.phoenixlab.dn.subfile.dnt.DntReader;
import co.phoenixlab.dn.util.LittleEndianDataOutputStream;
import com.divinitor.dn.lib.game.mod.DnAssetAccessService;
import com.divinitor.dn.lib.game.mod.definition.TableEditDirective;
import com.divinitor.dn.lib.game.mod.definition.TableRow;
import com.divinitor.dn.lib.game.mod.util.Utils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Queue;

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

        for (DntColumn column : dnt.getColumns()) {
            byte[] nameBytes = column.getName().getBytes(StandardCharsets.UTF_8);
            int len = nameBytes.length;
            dout.writeShort(len);
            dout.write(nameBytes);
            dout.write(column.getDataType().getId());
        }

        dout.write(body.data);

        dout.writeInt(5);
        dout.write("THEND".getBytes(StandardCharsets.UTF_8));

        dout.flush();

        return out.toByteArray();
    }

    private BodyResult buildBody(DntReader.DntHandle handle, TableEditDirective directive) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        TableRow[] add = directive.getAdd();
        Arrays.sort(add, Comparator.comparingInt(TableRow::getRowId));

        Queue<TableRow> additions = new ArrayDeque<>(add.length);
        additions.addAll(Arrays.asList(add));

        TIntObjectMap<TableRow> mod = new TIntObjectHashMap<>();
        for (TableRow modRow : directive.getModify()) {
            mod.put(modRow.getRowId(), modRow);
        }

        TIntSet delete = new TIntHashSet();
        delete.addAll(directive.getDelete());

        for (DntReader.DntHandle.RowReader rr : handle) {
            int thisRowId = rr.getRowId();

            //  Insert new
            TableRow candidate;
            while (!additions.isEmpty() && (candidate = additions.peek()).getRowId() < thisRowId) {
                //  Insert
                
            }

            //  Delete
            if (delete.contains(thisRowId)) {
                continue;
            }

            //  Edit
            if (mod.containsKey(thisRowId)) {
                //  Transform

            }

        }

        //  Tail entries
        while (!additions.isEmpty()) {
            //  Insert

        }

        BodyResult ret = new BodyResult();
        ret.data = outputStream.toByteArray();
        return ret;
    }

    static class BodyResult {
        byte[] data;
        int numRows;
    }
}
