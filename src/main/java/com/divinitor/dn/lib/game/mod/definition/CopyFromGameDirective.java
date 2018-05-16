package com.divinitor.dn.lib.game.mod.definition;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyFromGameDirective {

    protected String source;
    protected String dest;
    private Integer compressionLevel;

    @Override
    public String toString() {
        return "pak::" + source + " -> " + "pak::" + dest;
    }
}
