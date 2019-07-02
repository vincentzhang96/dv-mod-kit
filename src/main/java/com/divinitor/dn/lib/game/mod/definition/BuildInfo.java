package com.divinitor.dn.lib.game.mod.definition;


import com.github.zafarkhaja.semver.Version;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildInfo {

    protected Version kitVersion;
    protected CopyFromGameDirective[] copy;
    protected CopyFromPackDirective[] add;
    protected CopyFromFolderDirective[] folder;
    protected TableEditDirective[] editTable;
}
