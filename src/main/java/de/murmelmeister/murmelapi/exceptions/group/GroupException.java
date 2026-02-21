package de.murmelmeister.murmelapi.exceptions.group;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class GroupException extends MurmelException {
    public GroupException(String message) {
        super(message);
    }

    public GroupException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupException(Throwable cause) {
        super(cause);
    }
}
