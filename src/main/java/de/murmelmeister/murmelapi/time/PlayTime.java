package de.murmelmeister.murmelapi.time;

/**
 * Interface for managing playtime of users.
 * <p>
 * This interface provides methods to check if a user exists, create a new user, delete a user,
 * get the playtime of a user, update the playtime of a user, and add time to a user's playtime.
 */
public sealed interface PlayTime permits PlayTimeProvider {
    /**
     * Checks whether a playtime record exists for the given user.
     *
     * @param userId The user id to check for an existing playtime record.
     * @return {@code true} if the user id is greater than 0 and a record exists; {@code false} otherwise.
     */
    boolean existsUser(int userId);

    /**
     * Creates a playtime record for the specified user.
     *
     * @param userId The id of the user for whom the playtime record should be created.
     * @return The number of rows affected by the create operation, or 0 if userId is less than 1.
     */
    int createUser(int userId);

    /**
     * Deletes the playtime record of the specified user.
     *
     * @param userId The id of the user whose playtime record should be deleted.
     * @return The number of rows affected by the delete operation, or 0 if userId is less than 1.
     */
    int deleteUser(int userId);

    /**
     * Retrieves the playtime (in seconds) for the specified user.
     *
     * @param userId The id of the user whose playtime is to be retrieved.
     * @return The playtime in seconds if found; returns -1 if userId is less than 1 or the record does not exist.
     */
    int getTime(int userId);

    /**
     * Updates the playtime (in seconds) for the specified user.
     *
     * @param userId  The id of the user whose playtime should be updated.
     * @param seconds The new playtime value in seconds.
     */
    void updateTime(int userId, int seconds);

    /**
     * Increments the current playtime for the specified user by one second.
     *
     * @param userId The id of the user whose playtime should be incremented.
     */
    void addTime(int userId);
}
