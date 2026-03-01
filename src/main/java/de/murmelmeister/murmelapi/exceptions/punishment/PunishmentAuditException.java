package de.murmelmeister.murmelapi.exceptions.punishment;

public class PunishmentAuditException extends PunishmentException{
    public PunishmentAuditException(String message) {
        super(message);
    }

    public PunishmentAuditException(String message, Throwable cause) {
        super(message, cause);
    }

    public PunishmentAuditException(Throwable cause) {
        super(cause);
    }
}
