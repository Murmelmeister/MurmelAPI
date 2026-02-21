package de.murmelmeister.murmelapi.exceptions.clan;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class ClanException extends MurmelException {
    public ClanException(String message) {
        super(message);
    }

    public ClanException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClanException(Throwable cause) {
        super(cause);
    }
}
