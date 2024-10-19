package de.murmelmeister.murmelapi.time;

import java.util.List;

/**
 * JoinLogger interface defines the contract for logging and managing join dates of users.
 */
public sealed interface JoinLogger permits JoinLoggerProvider {
    /**
     * Logs the join date for a user identified by the given userId.
     *
     * @param userId The ID of the user whose join date is to be recorded
     */
    void createJoinDate(int userId);

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
     * Retrieves the join time for a user identified by the given userId and timeId.
     *
     * @param timeId The ID of the time record.
     * @param userId The ID of the user whose join time is being retrieved.
     * @return The join time in milliseconds since epoch for the specified user and timeId.
     */
    long getJoinTime(int timeId, int userId);

    /**
     * Retrieves the join date for a user identified by the given userId and timeId.
     *
     * @param timeId The ID of the time record.
     * @param userId The ID of the user whose join date is being retrieved.
     * @return The formatted join date string for the specified user and timeId.
     */
    String getJoinDate(int timeId, int userId);
}
