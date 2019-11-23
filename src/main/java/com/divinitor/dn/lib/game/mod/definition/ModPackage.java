package com.divinitor.dn.lib.game.mod.definition;

import com.divinitor.dn.lib.game.mod.ModKit;
import com.github.zafarkhaja.semver.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class ModPackage {

    private transient ModKit kit;

    /**
     * ID of the mod pack (should be unique for a given mod).
     */
    protected String id;

    /**
     * Display name for the mod pack.
     */
    protected String name;

    /**
     * Display description of the mod pack.
     */
    protected String description;

    /**
     * Version number.
     */
    protected Version version;

    /**
     * Author info.
     */
    protected String author;

    /**
     * Timestamp.
     */
    protected Instant timestamp;

    /**
     * Build information (used by ModKit to install/uninstall/manage the mod).
     */
    protected BuildInfo build;

    /**
     * (Optional) Custom prefix for the resulting pak file. Defaults to "00Resource_dvmk_".
     */
    protected String customPrefix;

    /**
     * (Optional) Project GitHub URL
     */
    protected String projectUrl;

    /**
     * (Optional) Divinitor Eris support
     */
    protected ErisInfo eris;

    /**
     * Whether or not to expand the compiled pak. Defaults to false
     */
    protected boolean expandPak;

    /**
     * Whether or not to output the result as a folder as well as a pak
     */
    protected boolean outputAsFolder;

    /**
     * (Optional) Custom suffix for the resulting pak file. Defaults to blank.
     */
    protected String customSuffix;

    @Setter
    protected transient boolean isLatest;

    @Override
    public String toString() {
        return id + ":" + version.toString();
    }

    public byte[] getAsset(String asset) throws IOException {
        return this.kit.getModPackageFile(this, asset);
    }

    public boolean hasAsset(String asset) {
        return this.kit.modPackageHasFile(this, asset);
    }
}
