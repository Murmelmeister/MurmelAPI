package de.murmelmeister.murmelapi.playtime;

import java.sql.SQLException;

/**
 * The PlayTime interface provides methods to manage and manipulate play time for users.
 */
public sealed interface PlayTime permits PlayTimeProvider {
    /**
     * Checks if a user with the given user ID exists.
     *
     * @param userId the ID of the user to check
     * @return true if a user with the given ID exists, false otherwise
     * @throws SQLException if there is an error accessing the database
     */
    boolean existsUser(int userId) throws SQLException;

    /**
     * Creates a new user with the given user ID.
     *
     * @param userId the ID of the user to create
     * @throws SQLException if there is an error accessing the database
     */
    void createUser(int userId) throws SQLException;

    /**
     * Deletes a user with the given user ID from the database.
     *
     * @param userId the ID of the user to delete
     * @throws SQLException if there is an error accessing the database
     */
    void deleteUser(int userId) throws SQLException;

    /**
     * Obtains the time for the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @return the time for the given user ID and play time type
     * @throws SQLException if there is an error accessing the database
     */
    long getTime(int userId, PlayTimeType type) throws SQLException;

    /**
     * Sets the play time for a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @param time   the play time to set for the user
     * @throws SQLException if there is an error accessing the database
     */
    void setTime(int userId, PlayTimeType type, long time) throws SQLException;

    /**
     * Adds 1 unit of time to the play time of a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @throws SQLException if there is an error accessing the database
     */
    void addTime(int userId, PlayTimeType type) throws SQLException;

    /**
     * Adds the specified amount of time to the play time of a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @param time   the amount of time to add to the user's play time, in milliseconds
     * @throws SQLException if there is an error accessing the database
     */
    void addTime(int userId, PlayTimeType type, long time) throws SQLException;

    /**
     * Removes the play time for a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @throws SQLException if there is an error accessing the database
     */
    void removeTime(int userId, PlayTimeType type) throws SQLException;

    /**
     * Removes the specified amount of time from the play time of a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @param time   the amount of time to remove from the user's play time, in milliseconds
     * @throws SQLException if there is an error accessing the database
     */
    void removeTime(int userId, PlayTimeType type, long time) throws SQLException;

    /**
     * Resets the play time for a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @throws SQLException if there is an error accessing the database
     */
    void resetTime(int userId, PlayTimeType type) throws SQLException;

    /**
     * Starts a timer for the specified user.
     *
     * @param userId the ID of the user for whom to start the timer
     * @throws SQLException if there is an error accessing the database
     */
    void timer(int userId) throws SQLException;
}
