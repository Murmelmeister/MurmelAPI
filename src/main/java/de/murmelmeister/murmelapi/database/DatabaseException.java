package de.murmelmeister.murmelapi.database;

public class DatabaseException extends RuntimeException {
    /**
     * Constructs a new DatabaseException with the specified detail message.
     *
     * @param message The detail message.
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Constructs a new DatabaseException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause   The cause of the exception.
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
