package de.murmelmeister.murmelapi.exceptions.language;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class LanguageException extends MurmelException {
    public LanguageException(String message) {
        super(message);
    }

    public LanguageException(String message, Throwable cause) {
        super(message, cause);
    }

    public LanguageException(Throwable cause) {
        super(cause);
    }
}
