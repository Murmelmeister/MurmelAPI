package de.murmelmeister.murmelapi.exceptions.punishment;

public class PunishmentException extends RuntimeException {
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
