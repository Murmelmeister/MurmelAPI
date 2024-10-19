package de.murmelmeister.murmelapi.time;

import java.util.List;

/**
 * QuitLogger interface defines the contract for logging and managing quit dates of users.
 */
public sealed interface QuitLogger permits QuitLoggerProvider {
    /**
     * Logs the quit date for a user identified by the given userId.
     *
     * @param userId The ID of the user whose quit date is to be recorded
     */
    void createQuitDate(int userId);

    /**
     * Deletes the user identified by the given userId from the system.
     *
     * @param userId The ID of the user to be deleted.
     */
    void deleteUser(int userId);

    /**
     * Retrieves a list of time IDs associated with a specific user.
     *
     * @param userId The ID of the user whose time IDs are to be retrieved.
     * @return A list of time IDs associated with the given user ID.
     */
    List<Integer> getTimeIds(int userId);

    /**
     * Retrieves the quit time for a user identified by the given userId and timeId.
     *
     * @param timeId The ID of the time record.
     * @param userId The ID of the user whose quit time is being retrieved.
     * @return The quit time in milliseconds since epoch for the specified user and timeId.
     */
    long getQuitTime(int timeId, int userId);

    /**
     * Retrieves the quit date for a user identified by the given userId and timeId.
     *
     * @param timeId The ID of the time record.
     * @param userId The ID of the user whose quit date is being retrieved.
     * @return The formatted quit date string for the specified user and timeId.
     */
    String getQuitDate(int timeId, int userId);
}
