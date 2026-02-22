package de.murmelmeister.murmelapi.exceptions.participant;

public class ParticipantPermissionException extends ParticipantException {
    public ParticipantPermissionException(String message) {
        super(message);
    }

    public ParticipantPermissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParticipantPermissionException(Throwable cause) {
        super(cause);
    }
}
