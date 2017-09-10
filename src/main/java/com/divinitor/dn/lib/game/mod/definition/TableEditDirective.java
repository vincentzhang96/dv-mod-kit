package com.divinitor.dn.lib.game.mod.definition;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TableEditDirective {

    protected String tableName;
    protected TableRow[] add;
    protected TableRow[] modify;
    protected int[] delete;
}
