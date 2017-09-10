package com.divinitor.dn.lib.game.mod.definition;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyFromPackDirective {

    protected String source;
    protected String dest;

    @Override
    public String toString() {
        return "mod::" + source + " -> " + "pak::" + dest;
    }

}
