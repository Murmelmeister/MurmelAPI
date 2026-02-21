package de.murmelmeister.murmelapi.exceptions.user;

public class UserStatsException extends UserException {
    public UserStatsException(String message) {
        super(message);
    }

    public UserStatsException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserStatsException(Throwable cause) {
        super(cause);
    }
}
