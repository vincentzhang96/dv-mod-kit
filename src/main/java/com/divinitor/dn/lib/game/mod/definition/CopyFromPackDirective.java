package com.divinitor.dn.lib.game.mod.definition;

import com.google.common.base.Strings;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyFromPackDirective {

    protected String source;
    protected String dest;
    protected String processor;

    @Override
    public String toString() {
        if (Strings.isNullOrEmpty(processor)) {
            return "mod::" + source + " -> " + "pak::" + dest;
        } else {
            return "mod::" + source + " -> " + processor + " -> " + "pak::" + dest;
        }
    }

}
