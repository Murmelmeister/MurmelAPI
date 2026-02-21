package de.murmelmeister.murmelapi.exceptions.clan;

public class ClanMemberException extends ClanException {
    public ClanMemberException(String message) {
        super(message);
    }

    public ClanMemberException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClanMemberException(Throwable cause) {
        super(cause);
    }
}
