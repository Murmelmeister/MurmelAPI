package de.murmelmeister.murmelapi.exceptions.friend;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class FriendException extends MurmelException {
    public FriendException(String message) {
        super(message);
    }

    public FriendException(String message, Throwable cause) {
        super(message, cause);
    }

    public FriendException(Throwable cause) {
        super(cause);
    }
}
