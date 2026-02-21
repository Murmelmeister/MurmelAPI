package de.murmelmeister.murmelapi.exceptions.language;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class MessageException extends MurmelException {
    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageException(Throwable cause) {
        super(cause);
    }
}
