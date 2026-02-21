package de.murmelmeister.murmelapi.exceptions.user;

public class UserExcuseException extends UserException {
    public UserExcuseException(String message) {
        super(message);
    }

    public UserExcuseException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserExcuseException(Throwable cause) {
        super(cause);
    }
}
