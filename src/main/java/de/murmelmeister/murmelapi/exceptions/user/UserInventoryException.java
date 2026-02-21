package de.murmelmeister.murmelapi.exceptions.user;

public class UserInventoryException extends UserException {
    public UserInventoryException(String message) {
        super(message);
    }

    public UserInventoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserInventoryException(Throwable cause) {
        super(cause);
    }
}
