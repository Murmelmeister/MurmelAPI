package de.murmelmeister.murmelapi.exceptions;

public class YamlMurmelException extends RuntimeException {
    public YamlMurmelException(String message) {
        super(message);
    }

    public YamlMurmelException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlMurmelException(Throwable cause) {
        super(cause);
    }
}
