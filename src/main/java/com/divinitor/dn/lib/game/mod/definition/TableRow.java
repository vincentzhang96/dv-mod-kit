package com.divinitor.dn.lib.game.mod.definition;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TableRow {

    protected int rowId;

    protected JsonObject columns;
}
