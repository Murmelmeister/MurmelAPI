package de.murmelmeister.murmelapi.exceptions.color;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class PrefixColorException extends MurmelException {
    public PrefixColorException(String message) {
        super(message);
    }

    public PrefixColorException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrefixColorException(Throwable cause) {
        super(cause);
    }
}
