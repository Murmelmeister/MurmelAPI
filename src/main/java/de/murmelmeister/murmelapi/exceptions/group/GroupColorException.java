package de.murmelmeister.murmelapi.exceptions.group;

public class GroupColorException extends GroupException {
    public GroupColorException(String message) {
        super(message);
    }

    public GroupColorException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupColorException(Throwable cause) {
        super(cause);
    }
}
