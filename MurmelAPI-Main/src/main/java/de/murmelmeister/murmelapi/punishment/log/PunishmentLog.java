package de.murmelmeister.murmelapi.punishment.log;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Represents a punishment log entry in the system.
 * <p>
 * A punishment log is a record of a punishment action taken against a user or IP address.
 * The log entry contains information such as the type of punishment, the reason for the punishment,
 * the expiration time of the punishment, and the user who executed the punishment.
 * <p>
 * Each punishment log entry is uniquely identified by a UUID and is associated with a specific user or IP address.
 * The log entry can be used to track the history of punishments applied to a user or IP address over time.
 */
public sealed interface PunishmentLog permits PunishmentLogProvider {
    /**
     * Checks if a log entry exists based on the specified log ID and type ID.
     *
     * @param logId  The unique identifier of the log entry
     * @param typeId The identifier for the type of the log entry
     * @return true if a log entry with the given log ID and type ID exists, otherwise false
     */
    boolean existsLog(UUID logId, int typeId);

    /**
     * Adds a punishment log associated with an IP address to the system.
     *
     * @param executorId  The ID of the user responsible for executing the action
     * @param typeId      The type ID of the punishment log
     * @param inetAddress The IP address associated with the punishment log
     * @param reasonId    The reason ID for the punishment
     * @return The unique identifier (UUID) of the newly created punishment log
     */
    UUID addLogIp(int executorId, int typeId, InetAddress inetAddress, int reasonId);

    /**
     * Adds a new log entry for a user to the punishment log.
     *
     * @param executorId  The ID of the user performing the action
     * @param typeId      The ID representing the type of log entry
     * @param userId      The ID of the target user associated with the log
     * @param inetAddress The IP address of the target user
     * @param reasonId    The ID representing the reason for the log entry
     * @return A UUID representing the unique identifier of the newly added log entry
     */
    UUID addLogUser(int executorId, int typeId, int userId, InetAddress inetAddress, int reasonId);

    /**
     * Deletes all logs associated with the specified user.
     *
     * @param userId The unique identifier of the user whose logs are to be deleted
     */
    void deleteUserLogs(int userId);

    /**
     * Retrieves a list of log identifiers associated with the specified user and punishment type.
     *
     * @param userId The unique identifier of the user whose logs are to be retrieved
     * @param typeId The type of punishment associated with the logs
     * @return A list of UUIDs representing the logs for the specified user and punishment type
     */
    List<UUID> getLogs(int userId, int typeId);

    /**
     * Retrieves a list of log identifiers associated with a specific IP address and type.
     *
     * @param inetAddress The IP address used to filter the logs
     * @param typeId      The type identifier used to filter the logs
     * @return A list of UUIDs representing the matching logs
     */
    List<UUID> getLogs(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the user ID associated with a specific punishment log.
     *
     * @param logId  The unique identifier of the punishment log
     * @param typeId The type identifier of the punishment log
     * @return The user ID associated with the specified punishment log
     */
    int getUserId(UUID logId, int typeId);

    /**
     * Retrieves the IP address associated with a specific punishment log.
     *
     * @param logId  The unique identifier of the log entry
     * @param typeId The type identifier of the log entry
     * @return The IP address as a string, or null if no IP address is associated with the specified log
     */
    String getIpAddress(UUID logId, int typeId);

    /**
     * Retrieves the reason ID associated with a specific punishment log entry.
     *
     * @param logId  The unique identifier of the punishment log.
     * @param typeId The type identifier of the punishment log.
     * @return The ID of the reason linked to the specified punishment log entry.
     */
    int getReasonId(UUID logId, int typeId);

    /**
     * Updates the reason ID for a specific punishment log entry.
     *
     * @param logId      The unique identifier of the punishment log entry to be updated.
     * @param typeId     The type identifier of the punishment log entry.
     * @param executorId The identifier of the user making the update.
     * @param reasonId   The new reason identifier to set for the log entry.
     */
    void setReasonId(UUID logId, int typeId, int executorId, int reasonId);

    /**
     * Retrieves the expiration time for a specific punishment log entry.
     *
     * @param logId  The unique identifier of the punishment log entry
     * @param typeId The type ID of the punishment log
     * @return A {@code Timestamp} object representing the expiration date and time of the association.
     */
    Timestamp getExpiredAt(UUID logId, int typeId);

    /**
     * Retrieves the expiration date of a punishment log entry.
     *
     * @param logId  The unique identifier of the punishment log entry
     * @param typeId The type identifier of the punishment log entry
     * @return A string representing the expiration date of the punishment log entry
     */
    String getExpiredDate(UUID logId, int typeId);

    /**
     * Sets the expiration time for a specific punishment log entry.
     *
     * @param logId      The unique identifier of the punishment log entry.
     * @param typeId     The type identifier of the punishment log entry.
     * @param executorId The identifier of the user making this change.
     * @param duration   The duration in milliseconds that determines the new expiration time.
     */
    void setExpiredAt(UUID logId, int typeId, int executorId, long duration);

    /**
     * Determines if the punishment log entry associated with the specified log ID and type ID is expired.
     *
     * @param logId  The unique identifier of the punishment log entry.
     * @param typeId The type identifier of the punishment log entry.
     * @return true if the punishment log entry is expired, otherwise false.
     */
    boolean isExpired(UUID logId, int typeId);

    /**
     * Retrieves the ID of the user or entity that created the specified punishment log entry.
     *
     * @param logId  The unique identifier of the punishment log entry
     * @param typeId The type identifier of the punishment log entry
     * @return The ID of the creator of the punishment log entry
     */
    int getCreatedBy(UUID logId, int typeId);

    /**
     * Retrieves the timestamp at which a punishment log was created.
     *
     * @param logId  The unique identifier of the punishment log
     * @param typeId The type identifier of the punishment log
     * @return The creation timestamp of the specified punishment log
     */
    Timestamp getCreatedAt(UUID logId, int typeId);

    /**
     * Retrieves the creation date of a punishment log entry.
     *
     * @param logId  The unique identifier of the punishment log entry
     * @param typeId The type identifier of the punishment log entry
     * @return A string representing the creation date of the punishment log entry
     */
    String getCreatedDate(UUID logId, int typeId);

    /**
     * Retrieves the ID of the user who last modified the punishment log entry.
     *
     * @param logId  The unique identifier of the punishment log entry
     * @param typeId The type identifier for the log entry
     * @return The ID of the user who last modified the log entry
     */
    int getModifiedBy(UUID logId, int typeId);

    /**
     * Retrieves the timestamp of when the specified punishment log was last modified.
     *
     * @param logId  The unique identifier of the punishment log.
     * @param typeId The type identifier of the punishment log.
     * @return The timestamp representing the last modification date and time of the specified log.
     */
    Timestamp getModifiedAt(UUID logId, int typeId);

    /**
     * Retrieves the modification date of a punishment log entry.
     *
     * @param logId  The unique identifier of the punishment log entry.
     * @param typeId The type identifier of the punishment log entry.
     * @return A string representing the modification date of the punishment log entry.
     */
    String getModifiedDate(UUID logId, int typeId);
}
