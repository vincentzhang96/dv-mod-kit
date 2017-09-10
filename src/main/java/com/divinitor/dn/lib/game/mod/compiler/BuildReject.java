package com.divinitor.dn.lib.game.mod.compiler;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildReject {

    private String path;
    private String reason;

}
