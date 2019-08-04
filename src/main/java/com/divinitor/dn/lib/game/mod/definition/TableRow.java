package com.divinitor.dn.lib.game.mod.definition;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableRow {

    protected int rowId;

    protected int[] rows;

    protected Map<String, Object> columns;
}
