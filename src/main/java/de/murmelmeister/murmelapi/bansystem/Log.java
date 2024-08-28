package de.murmelmeister.murmelapi.bansystem;

import java.util.List;

/**
 * The Log interface represents a logger that stores and retrieves log entries.
 */
public sealed interface Log permits LogProvider {
    /**
     * Checks if a log with the specified log ID exists.
     *
     * @param logId The ID of the log to check.
     * @return true if a log with the specified log ID exists, false otherwise.
     */
    boolean existsLog(int logId);

    /**
     * Adds a log entry to the logger.
     *
     * @param userId    The ID of the user associated with the log entry.
     * @param creatorId The ID of the user who created the log entry.
     * @param time      The timestamp of the log entry in milliseconds.
     * @param reasonId  The ID of the reason for the log entry.
     * @return The ID of the newly added log entry.
     */
    int addLog(int userId, int creatorId, long time, int reasonId);

    /**
     * Removes a log entry from the logger.
     *
     * @param logId The ID of the log entry to be removed.
     */
    void removeLog(int logId);

    /**
     * Deletes all logs associated with a specific user.
     *
     * @param userId The ID of the user for whom the logs will be deleted.
     */
    void deleteLog(int userId);

    /**
     * Retrieves the logs associated with the specified user ID.
     *
     * @param userId The ID of the user for whom the logs will be retrieved.
     * @return A list of integers representing the log IDs.
     */
    List<Integer> getLogs(int userId);

    /**
     * Retrieves the user ID associated with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @return The user ID associated with the specified log ID.
     */
    int getUserId(int logId);

    /**
     * Retrieves the ID of the user who created the log entry with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @return The ID of the user who created the log entry.
     */
    int getCreatorId(int logId);

    /**
     * Retrieves the created time of the log entry with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @return The created time of the log entry, represented as a long value in milliseconds.
     */
    long getCreatedTime(int logId);

    /**
     * Retrieves the created date of the log entry with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @return The created date of the log entry, represented as a string.
     */
    String getCreatedDate(int logId);

    /**
     * Retrieves the expiration time of the log entry with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @return The expiration time of the log entry, represented as a long value in milliseconds.
     */
    long getExpiredTime(int logId);

    /**
     * Retrieves the expiration date of the log entry with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @return The expiration date of the log entry, represented as a string.
     */
    String getExpiredDate(int logId);

    /**
     * Sets the expiration time for a log entry with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @param time  The new expiration time in milliseconds.
     * @return A string representing the result of setting the expiration time. Returns an empty string if successful.
     */
    String setExpiredTime(int logId, long time);

    /**
     * Adds an expired time to the log entry with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @param time  The expired time to be added, represented as a long value in milliseconds.
     * @return A string representing the result of adding the expired time. Returns an empty string if successful.
     */
    String addExpiredTime(int logId, long time);

    /**
     * Removes the expired time from a log entry with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @param time  The expired time to be removed, represented as a long value in milliseconds.
     * @return A string representing the result of removing the expired time. Returns an empty string if successful.
     */
    String removeExpiredTime(int logId, long time);

    /**
     * Retrieves the reason ID associated with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @return The reason ID associated with the specified log ID.
     */
    int getReasonId(int logId);

    /**
     * Sets the reason ID for a log entry with the specified log ID.
     *
     * @param logId    The ID of the log entry.
     * @param reasonId The reason ID to be set for the log entry.
     */
    void setReasonId(int logId, int reasonId);

    /**
     * Retrieves the reason associated with the specified log ID.
     *
     * @param logId The ID of the log entry.
     * @return The reason associated with the specified log ID, represented as a string.
     */
    String getReason(int logId);
}
