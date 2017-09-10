package com.divinitor.dn.lib.game.mod.pak;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagedPakIndexEntry {
    public static final int SIZEOF_ENTRY = 316;
    public static final int SIZEOF_FILE_PATH = 256;
    public static final int SIZEOF_UNKNOWN_B = 40;
    public static final int SIZEOF_REMAINDER = 40 - 8;
    public static final byte[] REMAINDER_INSTANCE = new byte[SIZEOF_REMAINDER];

    /**
     * This file's virtual path.
     */
    String filePath;
    /**
     * The raw (disk) size of this file, as it exists in the pak.
     */
    int rawSize;
    /**
     * The real (decompressed) size of this file. This value may be 0, meaning that the decompressed size is
     * unknown.
     */
    int realSize;

    /**
     * The compressed (disk) size of this file, as it exists in the pak. This should be the same as {@link #rawSize}.
     */
    int compressedSize;
    /**
     * The offset to this file's data within the pak.
     */
    int offset;
    /**
     * Unknown value. Seems to be a checksum? Has no effect if left at 0.
     */
    int unknownA;

    //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //
    //  unknownB is 40 bytes that we're using to store metadata about the entry
    //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //  //

    /**
     * A hash of the file content (xxHash)
     */
    long contentHash;

    /**
     * Unused header space
     */
    byte[] remainder;
}
