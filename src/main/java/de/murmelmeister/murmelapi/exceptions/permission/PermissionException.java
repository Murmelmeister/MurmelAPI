package de.murmelmeister.murmelapi.exceptions.permission;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class PermissionException extends MurmelException {
    public PermissionException(String message) {
        super(message);
    }

    public PermissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermissionException(Throwable cause) {
        super(cause);
    }
}
