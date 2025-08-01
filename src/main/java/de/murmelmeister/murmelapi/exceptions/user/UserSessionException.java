package de.murmelmeister.murmelapi.exceptions.user;

public class UserSessionException extends UserException {
    public UserSessionException(String message) {
        super(message);
    }

    public UserSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserSessionException(Throwable cause) {
        super(cause);
    }
}
