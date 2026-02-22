package de.murmelmeister.murmelapi.exceptions.participant;

public class ParticipantParentException extends ParticipantException {
    public ParticipantParentException(String message) {
        super(message);
    }

    public ParticipantParentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParticipantParentException(Throwable cause) {
        super(cause);
    }
}
