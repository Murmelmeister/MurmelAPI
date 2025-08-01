package de.murmelmeister.murmelapi.exceptions.user;

public class UserPlayTimeException extends UserException {
    public UserPlayTimeException(String message) {
        super(message);
    }

    public UserPlayTimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserPlayTimeException(Throwable cause) {
        super(cause);
    }
}
