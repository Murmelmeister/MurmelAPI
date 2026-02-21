package de.murmelmeister.murmelapi.exceptions.clan;

public class ClanGroupException extends ClanException {
    public ClanGroupException(String message) {
        super(message);
    }

    public ClanGroupException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClanGroupException(Throwable cause) {
        super(cause);
    }
}
