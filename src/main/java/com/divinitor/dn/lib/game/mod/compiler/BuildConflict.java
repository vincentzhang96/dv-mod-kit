package com.divinitor.dn.lib.game.mod.compiler;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildConflict {

    protected String file;
    protected Set<String> conflictingModIds;
}
