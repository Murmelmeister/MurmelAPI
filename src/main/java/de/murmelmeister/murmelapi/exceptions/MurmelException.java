package de.murmelmeister.murmelapi.exceptions;

public class MurmelException extends RuntimeException {
    public MurmelException(String message) {
        super(message);
    }

    public MurmelException(String message, Throwable cause) {
        super(message, cause);
    }

    public MurmelException(Throwable cause) {
        super(cause);
    }
}
