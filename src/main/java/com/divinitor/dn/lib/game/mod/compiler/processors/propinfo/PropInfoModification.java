package com.divinitor.dn.lib.game.mod.compiler.processors.propinfo;

import co.phoenixlab.dn.util.math.Vec3;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropInfoModification {

    public enum Action {
        @SerializedName("DELETE")
        DELETE,
        @SerializedName("MODIFY")
        MODIFY,
    }

    private String sknName;
    private int propId;
    private Action action;

    private String replaceSkn;
    private Vec3 pos;
    private Vec3 rot;
    private Vec3 scale;
}
