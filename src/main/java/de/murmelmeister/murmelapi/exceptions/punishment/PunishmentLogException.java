package de.murmelmeister.murmelapi.exceptions.punishment;

public class PunishmentLogException extends PunishmentException{
    public PunishmentLogException(String message) {
        super(message);
    }

    public PunishmentLogException(String message, Throwable cause) {
        super(message, cause);
    }

    public PunishmentLogException(Throwable cause) {
        super(cause);
    }
}
