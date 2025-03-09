package de.murmelmeister.murmelapi.time;

/**
 * The PlayTime interface provides methods to manage and manipulate play time for users.
 */
public sealed interface PlayTime permits PlayTimeProvider {
    /**
     * Checks if a user with the specified user ID exists in the playtime system.
     *
     * @param userId The ID of the user to check for existence.
     * @return {@code true} if the user exists, {@code false} otherwise.
     */
    boolean existsUser(int userId);

    /**
     * Creates a user with the specified user ID in the playtime system.
     * If the user already exists, no action is taken.
     *
     * @param userId The ID of the user to create
     */
    void createUser(int userId);

    /**
     * Deletes a user with the specified user ID from the playtime system.
     *
     * @param userId The ID of the user to delete
     */
    void deleteUser(int userId);

    /**
     * Retrieves the play time, in seconds, of a user identified by the specified user ID.
     *
     * @param userId The ID of the user whose play time is being retrieved.
     * @return The play time of the user in seconds, or -1 if the user ID does not exist in the database.
     */
    int getTime(int userId);

    /**
     * Sets the play time for a specific user.
     *
     * @param userId The ID of the user whose play time is being set.
     * @param time   The new play time value to set, specified in seconds.
     */
    void setTime(int userId, int time);

    /**
     * Increments the play time for a user with the specified user ID by one unit.
     *
     * @param userId The ID of the user whose play time will be incremented
     */
    void addTime(int userId);

    /**
     * Adds a specified amount of play time for a user based on the given play time type.
     *
     * @param userId The ID of the user whose play time will be increased
     * @param type   The type of play time to be added (e.g., seconds, minutes, hours, etc.)
     * @param time   The amount of play time to add, specified in the given play time type
     */
    void addTime(int userId, PlayTimeType type, int time);

    /**
     * Removes one unit of play time from the play time of the user with the specified user ID.
     *
     * @param userId The ID of the user whose play time will be decreased by one unit
     */
    void removeTime(int userId);

    /**
     * Removes a specified amount of play time for a user with the given user ID and play time type.
     *
     * @param userId The ID of the user whose play time will be reduced
     * @param type   The type of play time to be removed (e.g., seconds, minutes, hours)
     * @param time   The amount of time to be removed, specified in the given play time type
     */
    void removeTime(int userId, PlayTimeType type, int time);

    /**
     * Resets the playtime of a user with the specified user ID to zero.
     *
     * @param userId The ID of the user whose playtime is to be reset
     */
    void resetTime(int userId);
}
