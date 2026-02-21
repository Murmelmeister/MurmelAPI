package de.murmelmeister.murmelapi.exceptions.user;

public class UserPrefixColorException extends UserException {
    public UserPrefixColorException(String message) {
        super(message);
    }

    public UserPrefixColorException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserPrefixColorException(Throwable cause) {
        super(cause);
    }
}
