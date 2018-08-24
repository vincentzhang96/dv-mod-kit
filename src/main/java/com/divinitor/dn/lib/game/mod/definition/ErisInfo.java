package com.divinitor.dn.lib.game.mod.definition;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErisInfo {

    protected int majorVersion;

    protected int minorVersion;

    /**
     * Optional xor key. If 0, one will be generated.
     */
    protected int xorKey;

    // TODO constraints and anti-tamper
}
