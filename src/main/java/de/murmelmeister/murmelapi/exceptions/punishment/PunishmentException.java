package de.murmelmeister.murmelapi.exceptions.punishment;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class PunishmentException extends MurmelException {
    public PunishmentException(String message) {
        super(message);
    }

    public PunishmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public PunishmentException(Throwable cause) {
        super(cause);
    }
}
