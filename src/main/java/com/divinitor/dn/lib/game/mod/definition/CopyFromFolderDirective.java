package com.divinitor.dn.lib.game.mod.definition;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyFromFolderDirective {
    protected String source;
    protected String dest;

    @Override
    public String toString() {
        return "pak::" + source + "/* -> " + "pak::" + dest + "/*";
    }
}
