package de.murmelmeister.murmelapi.exceptions.punishment;

public class PunishmentIpAddressException extends PunishmentException {
    public PunishmentIpAddressException(String message) {
        super(message);
    }

    public PunishmentIpAddressException(String message, Throwable cause) {
        super(message, cause);
    }

    public PunishmentIpAddressException(Throwable cause) {
        super(cause);
    }
}
