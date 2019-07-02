package com.divinitor.dn.lib.game.mod.compiler.processors.propinfo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PropInfoDescriptor {

    private String mapName;
    private List<PropInfoModification> mods;

}
