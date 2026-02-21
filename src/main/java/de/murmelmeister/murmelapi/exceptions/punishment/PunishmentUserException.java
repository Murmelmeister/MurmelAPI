package de.murmelmeister.murmelapi.exceptions.punishment;

public class PunishmentUserException extends PunishmentException {
    public PunishmentUserException(String message) {
        super(message);
    }

  public PunishmentUserException(String message, Throwable cause) {
    super(message, cause);
  }

  public PunishmentUserException(Throwable cause) {
    super(cause);
  }
}
