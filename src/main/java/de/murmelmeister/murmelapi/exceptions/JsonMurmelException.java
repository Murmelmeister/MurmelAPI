package de.murmelmeister.murmelapi.exceptions;

public class JsonMurmelException extends RuntimeException {
    public JsonMurmelException(String message) {
        super(message);
    }

    public JsonMurmelException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonMurmelException(Throwable cause) {
        super(cause);
    }
}
