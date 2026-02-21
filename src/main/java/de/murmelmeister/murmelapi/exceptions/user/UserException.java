package de.murmelmeister.murmelapi.exceptions.user;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class UserException extends MurmelException {
    public UserException(String message) {
        super(message);
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserException(Throwable cause) {
        super(cause);
    }
}
