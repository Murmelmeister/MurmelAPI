package de.murmelmeister.murmelapi.exceptions.user;

public class UserLoginException extends UserException {
    public UserLoginException(String message) {
        super(message);
    }

    public UserLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserLoginException(Throwable cause) {
        super(cause);
    }
}
