package de.murmelmeister.murmelapi.exceptions.participant;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class ParticipantException extends MurmelException {
    public ParticipantException(String message) {
        super(message);
    }

    public ParticipantException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParticipantException(Throwable cause) {
        super(cause);
    }
}
