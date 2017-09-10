package com.divinitor.dn.lib.game.mod.pak;

import com.github.zafarkhaja.semver.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ManagedPak is an extension to the Eyedentity Games PAK format which adds a few fields and indices in a binary
 * compatible way to store information on installed mods.
 *
 * The format adds several new fields by chopping the MAGIC_NUMBER in half. Offset 128 begins the new fields.
 */
@Getter
@Setter
@NoArgsConstructor
public class ManagedPak {

    /**
     * Size of the Pak header.
     */
    public static final int SIZEOF_HEADER = 1024;
    /**
     * Size of the Pak magic number/string.
     *
     * @see ManagedPak#MAGIC_NUMBER
     */
    public static final int SIZEOF_MAGIC_NUMBER = 128;
    /**
     * The Pak magic number/string
     */
    public static final String MAGIC_NUMBER = "EyedentityGames Packing File 0.1\nDivinitor ModKit Managed Package";

    public static final int SIZEOF_BUFFER = 128 - (3 * 4);
    /**
     * The currently supported Pak version
     */
    public static final int CURRENT_VERSION = 11;

    public static final Version CURRENT_MANAGED_VERSION = Version.forIntegers(1, 0);

    /**
     * Half of a gigabyte.
     */
    public static final long HALF_GIGABYTE = 512L * 1024L * 1024L;

    /**
     * Pak file magic number/identifier string. Should always be "EyedentityGames Packing File 0.1".
     */
    String magicNumber;

    /**
     * Pak file version. Should be 11 (0x0B).
     */
    int version;

    /**
     * Number of files in this pak file.
     */
    int fileCount;

    /**
     * Offset to the file index.
     */
    int fileIndexTableOffset;

    /**
     * Managed Pak major version number.
     * @see ManagedPak#CURRENT_MANAGED_VERSION
     */
    int managedMajorVersion;

    /**
     * Managed Pak minor version number.
     * @see ManagedPak#CURRENT_MANAGED_VERSION
     */
    int managedMinorVersion;

    /**
     * Number of modpacks in this pak file.
     */
    int modPackCount;

    /**
     * Offset to the modpack file index
     */
    int modPackIndexTableOffset;

    /**
     * File index.
     */
    ManagedPakIndexEntry[] fileIndex;

    /**
     * Mod index.
     */
    ManagedPakModIndexEntry[] modIndex;
}
