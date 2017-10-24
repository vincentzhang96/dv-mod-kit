package com.divinitor.dn.lib.game.mod.compiler;

import co.phoenixlab.dn.subfile.dnt.Dnt;
import co.phoenixlab.dn.subfile.dnt.DntColumn;
import co.phoenixlab.dn.subfile.dnt.DntReader;
import co.phoenixlab.dn.util.DnStringUtils;
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
import java.util.*;

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

    private BodyResult buildBody(DntReader.DntHandle handle, TableEditDirective directive) throws IOException {
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

        int rows = 0;

        for (DntReader.DntHandle.RowReader rr : handle) {
            int thisRowId = rr.getRowId();

            //  Insert new
            while (!additions.isEmpty() && additions.peek().getRowId() < thisRowId) {
                //  Insert
                outputStream.write(this.insertion(handle, additions.poll()));
                ++rows;
            }

            //  Delete
            if (delete.contains(thisRowId)) {
                continue;
            }

            //  Edit
            if (mod.containsKey(thisRowId)) {
                //  Transform

            }


            ++rows;
        }

        //  Tail entries
        while (!additions.isEmpty()) {
            //  Insert
            outputStream.write(this.insertion(handle, additions.poll()));
            ++rows;
        }

        BodyResult ret = new BodyResult();
        ret.data = outputStream.toByteArray();
        return ret;
    }

    private byte[] insertion(DntReader.DntHandle handle, TableRow row) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        LittleEndianDataOutputStream dout = new LittleEndianDataOutputStream(outputStream);

        dout.writeInt(row.getRowId());

        Map<String, Object> columns = row.getColumns();
        for (DntColumn dntColumn : handle.getDnt().getColumns()) {
            Object val = columns.get(dntColumn.getName());

            switch (dntColumn.getDataType()) {
                case BOOLEAN: {
                    boolean value = parseBool(val);
                    dout.writeInt(value ? 1 : 0);
                    break;
                }
                case TEXT: {
                    String value = val == null ? "" : String.valueOf(val);
                    DnStringUtils.writeShortLengthPrefixedString(value, dout);
                    break;
                }
                case FLOAT:
                case DOUBLE: {
                    float value = parseFloat(val);
                    dout.writeFloat(value);
                    break;
                }
                case INTEGER: {
                    int value = parseInt(val);
                    dout.writeInt(value);
                    break;
                }
                default:
                    throw new IllegalArgumentException();
            }
        }
        
        return outputStream.toByteArray();
    }

    private static boolean parseBool(Object o) {
        if (o instanceof Boolean) {
            return (boolean) o;
        }

        if (o instanceof Number) {
            return ((Number) o).intValue() != 0;
        }

        if (o instanceof String) {
            return Boolean.valueOf((String) o);
        }

        return o != null;
    }

    private static float parseFloat(Object o) {
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }

        if (o instanceof String) {
            return Float.parseFloat((String) o);
        }

        return 0F;
    }

    private static int parseInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }

        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }

        return 0;
    }

    static class BodyResult {
        byte[] data;
        int numRows;
    }
}
