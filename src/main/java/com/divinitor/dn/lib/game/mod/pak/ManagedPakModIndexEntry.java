package com.divinitor.dn.lib.game.mod.pak;

import com.github.zafarkhaja.semver.Version;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagedPakModIndexEntry {

    public static final int SIZEOF_ID = 32;
    public static final int SIZEOF_NAME = 64;
    public static final int SIZEOF_VERSION = 32;
    public static final int SIZEOF_PROJECTURL = 64;

    /**
     * ID of the mod pack (should be unique for a given mod).
     */
    protected String id;

    /**
     * Display name for the mod pack.
     */
    protected String name;

    /**
     * Version of the mod pack.
     */
    protected Version version;

    /**
     * Pack GitHub URL
     */
    protected String projectUrl;
}
