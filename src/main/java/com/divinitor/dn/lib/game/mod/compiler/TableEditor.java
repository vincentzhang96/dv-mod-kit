package com.divinitor.dn.lib.game.mod.compiler;

import co.phoenixlab.dn.subfile.dnt.Dnt;
import co.phoenixlab.dn.subfile.dnt.DntColumn;
import co.phoenixlab.dn.subfile.dnt.DntReader;
import co.phoenixlab.dn.util.LittleEndianDataOutputStream;
import com.divinitor.dn.lib.game.mod.DnAssetAccessService;
import com.divinitor.dn.lib.game.mod.definition.ModPackage;
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
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TableEditor {

    private final DnAssetAccessService assetAccessService;

    public TableEditor(DnAssetAccessService assetAccessService) {
        this.assetAccessService = assetAccessService;
    }

    public Utils.ThrowingSupplier<byte[]> tableEdit(
        String tableName,
        TableEditDirective directive,
        ModPackage modPack) {
        return () -> this.compileTable(tableName, directive, modPack);
    }

    public byte[] compileTable(String tableName, TableEditDirective directive, ModPackage modPack) throws IOException {
        byte[] tableBytes;
        if (tableName.startsWith("!")) {
            tableBytes = modPack.getAsset(tableName.substring(1));
        } else {
            tableBytes = this.assetAccessService.getAsset(tableName);
        }

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

        dout.write(5);
        dout.write("THEND".getBytes(StandardCharsets.UTF_8));

        dout.flush();

        return out.toByteArray();
    }

    private BodyResult buildBody(DntReader.DntHandle handle, TableEditDirective directive) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        TableRow[] add = directive.getAdd();
        if (add == null) {
            add = new TableRow[0];
        }

        Arrays.sort(add, Comparator.comparingInt(TableRow::getRowId));

        Queue<TableRow> additions = new ArrayDeque<>(add.length);
        additions.addAll(Arrays.asList(add));

        TIntObjectMap<TableRow> mod = new TIntObjectHashMap<>();
        TableRow[] modify = directive.getModify();
        if (modify == null) {
            modify = new TableRow[0];
        }

        for (TableRow modRow : modify) {
            int[] rows = modRow.getRows();
            if (rows != null && rows.length > 0) {
                for (int row : rows) {
                    mod.put(row, new TableRow(row, null, modRow.getColumns()));
                }
            }
            if (modRow.getRowId() > 0) {
                mod.put(modRow.getRowId(), modRow);
            }
        }

        TIntSet delete = new TIntHashSet();

        int[] deleteIds = directive.getDelete();
        if (deleteIds != null) {
            delete.addAll(deleteIds);
        }

        int rows = 0;
        Dnt dnt = handle.getDnt();
        ByteBuffer data = dnt.getData();
        data.order(ByteOrder.LITTLE_ENDIAN);
        int[] widths = new int[dnt.getNumColumns()];
        for (int i = 0; i < dnt.getColumns().length; i++) {
            widths[i] = dnt.getColumns()[i].getDataType() == DntColumn.DataType.TEXT ? 0 : 4;
        }

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
                outputStream.write(this.modify(handle, mod.get(thisRowId), rr));
            } else {
                //  Copy
                int startPos, offset;
                startPos = offset = handle.getRowOffset(thisRowId);
                offset += 4;
                for (int width : widths) {
                    if (width == 0) {
                        int addL = data.get(offset) & 0xFF;
                        addL |= (data.get(offset + 1) << 8) & 0xFF00;
                        offset += addL;
                        offset += 2;
                    } else {
                        offset += width;
                    }
                }

                int width = offset - startPos;
                byte[] write = new byte[width];
                data.position(startPos);
                data.get(write, 0, width);
                outputStream.write(write, 0, width);
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
        ret.numRows = rows;
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
                    this.writeDntString(dout, value);
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

        dout.flush();
        return outputStream.toByteArray();
    }

    private void writeDntString(LittleEndianDataOutputStream dout, String value) throws IOException {
        byte[] buf = value.getBytes(StandardCharsets.UTF_8);
        dout.writeShort(buf.length);
        dout.write(buf);
    }

    private byte[] modify(DntReader.DntHandle handle, TableRow row, DntReader.DntHandle.RowReader rr)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        LittleEndianDataOutputStream dout = new LittleEndianDataOutputStream(outputStream);

        dout.writeInt(row.getRowId());

        Map<String, Object> columns = row.getColumns();
        DntColumn[] columns1 = handle.getDnt().getColumns();
        for (int i = 0, columns1Length = columns1.length; i < columns1Length; i++) {
            DntColumn dntColumn = columns1[i];
            Object val = columns.get(dntColumn.getName());

            if (val == null) {
                switch (dntColumn.getDataType()) {
                    case BOOLEAN: {
                        boolean value = rr.getBoolean(i);
                        dout.writeInt(value ? 1 : 0);
                        break;
                    }
                    case TEXT: {
                        String value = rr.getString(i);
                        this.writeDntString(dout, value);
                        break;
                    }
                    case FLOAT:
                    case DOUBLE: {
                        float value = rr.getFloat(i);
                        dout.writeFloat(value);
                        break;
                    }
                    case INTEGER: {
                        int value = rr.getInt(i);
                        dout.writeInt(value);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException();
                }
            } else {
                switch (dntColumn.getDataType()) {
                    case BOOLEAN: {
                        boolean value = parseBool(val);
                        dout.writeInt(value ? 1 : 0);
                        break;
                    }
                    case TEXT: {
                        String value = String.valueOf(val);
                        this.writeDntString(dout, value);
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
