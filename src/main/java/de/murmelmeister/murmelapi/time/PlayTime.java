package de.murmelmeister.murmelapi.time;

/**
 * The PlayTime interface provides methods to manage and manipulate play time for users.
 */
public sealed interface PlayTime permits PlayTimeProvider {
    /**
     * Checks if a user with the given user ID exists.
     *
     * @param userId The ID of the user to check
     * @return true if a user with the given ID exists, false otherwise
     */
    boolean existsUser(int userId);

    /**
     * Creates a new user with the given user ID.
     *
     * @param userId The ID of the user to create
     */
    void createUser(int userId);

    /**
     * Deletes a user with the given user ID from the database.
     *
     * @param userId The ID of the user to delete
     */
    void deleteUser(int userId);

    /**
     * Obtains the time for the given user ID and play time type.
     *
     * @param userId The ID of the user
     * @return the time for the given user ID and play time type
     */
    int getTime(int userId);

    /**
     * Sets the play time for a user with the specified user ID.
     *
     * @param userId The ID of the user
     * @param time   The play time to set for the user, in seconds
     */
    void setTime(int userId, int time);

    /**
     * Adds one unit of play time to the play time of a user with the given user ID.
     *
     * @param userId The ID of the user whose play time will be incremented by one unit
     */
    void addTime(int userId);

    /**
     * Adds the specified amount of time to the play time of a user with the given user ID and play time type.
     *
     * @param userId The ID of the user
     * @param type   The type of play time
     * @param time   The amount of time to add to the user's play time, in milliseconds
     */
    void addTime(int userId, PlayTimeType type, int time);

    /**
     * Removes one unit of play time from the play time of a user specified with the given user ID.
     *
     * @param userId The ID of the user whose play time will be decremented
     */
    void removeTime(int userId);

    /**
     * Removes the specified amount of time from the play time of a user with the given user ID and play time type.
     *
     * @param userId The ID of the user
     * @param type   The type of play time
     * @param time   The amount of time to remove from the user's play time, in milliseconds
     */
    void removeTime(int userId, PlayTimeType type, int time);

    /**
     * Resets the play time for a user with the given user ID to zero.
     *
     * @param userId The ID of the user whose play time needs to be reset
     */
    void resetTime(int userId);
}
