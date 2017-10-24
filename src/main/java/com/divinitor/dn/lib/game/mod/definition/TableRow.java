package com.divinitor.dn.lib.game.mod.definition;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TableRow {

    protected int rowId;

    protected Map<String, Object> columns;
}
