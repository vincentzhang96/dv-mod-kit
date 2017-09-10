package com.divinitor.dn.lib.game.mod;

import java.nio.file.Path;

public class NotGameDirectoryException extends RuntimeException {

    public NotGameDirectoryException() {
    }

    public NotGameDirectoryException(Path path) {
        super(path.toAbsolutePath().toString() + " is not a valid DN game directory");
    }

    public NotGameDirectoryException(Path path, String reason) {
        super(path.toAbsolutePath().toString() + " is not a valid DN game directory: " + reason);
    }

    public NotGameDirectoryException(Path path, Throwable cause) {
        super(path.toAbsolutePath().toString() + " is not a valid DN game directory", cause);
    }

    public NotGameDirectoryException(Path path, String reason, Throwable cause) {
        super(path.toAbsolutePath().toString() + " is not a valid DN game directory: " + reason, cause);
    }

    public NotGameDirectoryException(Throwable cause) {
        super(cause);
    }
}
