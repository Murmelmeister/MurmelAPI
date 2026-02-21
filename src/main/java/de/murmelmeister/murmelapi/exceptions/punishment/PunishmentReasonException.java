package de.murmelmeister.murmelapi.exceptions.punishment;

public class PunishmentReasonException extends PunishmentException {
    public PunishmentReasonException(String message) {
        super(message);
    }

    public PunishmentReasonException(String message, Throwable cause) {
        super(message, cause);
    }

    public PunishmentReasonException(Throwable cause) {
        super(cause);
    }
}
