package de.murmelmeister.murmelapi.playtime;

/**
 * The PlayTime interface provides methods to manage and manipulate play time for users.
 */
public sealed interface PlayTime permits PlayTimeProvider {
    /**
     * Checks if a user with the given user ID exists.
     *
     * @param userId the ID of the user to check
     * @return true if a user with the given ID exists, false otherwise
     */
    boolean existsUser(int userId);

    /**
     * Creates a new user with the given user ID.
     *
     * @param userId the ID of the user to create
     */
    void createUser(int userId);

    /**
     * Deletes a user with the given user ID from the database.
     *
     * @param userId the ID of the user to delete
     */
    void deleteUser(int userId);

    /**
     * Obtains the time for the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @return the time for the given user ID and play time type
     */
    long getTime(int userId, PlayTimeType type);

    /**
     * Sets the play time for a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @param time   the play time to set for the user
     */
    void setTime(int userId, PlayTimeType type, long time);

    /**
     * Adds 1 unit of time to the play time of a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     */
    void addTime(int userId, PlayTimeType type);

    /**
     * Adds the specified amount of time to the play time of a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @param time   the amount of time to add to the user's play time, in milliseconds
     */
    void addTime(int userId, PlayTimeType type, long time);

    /**
     * Removes the play time for a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     */
    void removeTime(int userId, PlayTimeType type);

    /**
     * Removes the specified amount of time from the play time of a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     * @param time   the amount of time to remove from the user's play time, in milliseconds
     */
    void removeTime(int userId, PlayTimeType type, long time);

    /**
     * Resets the play time for a user with the given user ID and play time type.
     *
     * @param userId the ID of the user
     * @param type   the type of play time
     */
    void resetTime(int userId, PlayTimeType type);

    /**
     * Starts a timer for the specified user.
     *
     * @param userId the ID of the user for whom to start the timer
     */
    void timer(int userId);
}
