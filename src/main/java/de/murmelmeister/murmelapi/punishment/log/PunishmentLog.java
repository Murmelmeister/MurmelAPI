package de.murmelmeister.murmelapi.punishment.log;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * PunishmentLog interface to manage punishment logs in the database.
 * This interface provides methods to interact with punishment log data.
 */
public sealed interface PunishmentLog permits PunishmentLogProvider {
    /**
     * Checks whether a punishment log exists for the given UUID.
     *
     * @param id The UUID representing the punishment log record.
     * @return {@code true} if the log exists; {@code false} otherwise.
     */
    boolean existsLog(UUID id);

    /**
     * Adds a new punishment log for a user.
     * The punishment reason's duration is used to calculate the expiration timestamp.
     *
     * @param reasonId  The identifier of the punishment reason.
     * @param userId    The identifier of the punished user.
     * @param ipAddress The IP address associated with the punishment.
     * @param createdBy The identifier of the user creating this log.
     * @return The UUID of the newly created punishment log.
     */
    UUID addLogUser(int reasonId, int userId, String ipAddress, int createdBy);

    /**
     * Adds a new punishment log for an IP address.
     * The punishment reason's duration is used to calculate the expiration timestamp.
     *
     * @param reasonId  The identifier of the punishment reason.
     * @param ipAddress The IP address being punished.
     * @param createdBy The identifier of the user creating this log.
     * @return The UUID of the newly created punishment log.
     */
    UUID addLogIp(int reasonId, String ipAddress, int createdBy);

    /**
     * Deletes all punishment logs associated with the specified user.
     *
     * @param userId The identifier of the user whose logs should be deleted.
     * @return The number of rows affected by the deletion.
     */
    int deleteUserLogs(int userId);

    /**
     * Retrieves a list of punishment log UUIDs for the specified user.
     *
     * @param userId The identifier of the user.
     * @return A List of UUIDs corresponding to the user's punishment logs, or {@code null} if userId is invalid.
     */
    List<UUID> getLogsByUser(int userId);

    /**
     * Retrieves a list of punishment log UUIDs for the specified IP address.
     *
     * @param ipAddress The IP address to query.
     * @return A List of UUIDs corresponding to the punishment logs associated with the IP, or {@code null} if ipAddress is null.
     */
    List<UUID> getLogsByIp(String ipAddress);

    /**
     * Retrieves the user ID associated with a punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return The user ID if found; -2 otherwise.
     */
    int getUserId(UUID id);

    /**
     * Retrieves the IP address associated with a punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return The IP address as a String, or {@code null} if not found.
     */
    String getIpAddress(UUID id);

    /**
     * Retrieves the punishment reason ID associated with the specified punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return The punishment reason ID if found; -1 otherwise.
     */
    int getReasonId(UUID id);

    /**
     * Updates the punishment reason for a specific log.
     *
     * @param id        The UUID of the punishment log record.
     * @param reasonId  The new punishment reason ID.
     * @param updatedBy The ID of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setReason(UUID id, int reasonId, int updatedBy);

    /**
     * Retrieves the expiration timestamp for a punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return The expiration Timestamp, or {@code null} if not found.
     */
    Timestamp getExpiredAt(UUID id);

    /**
     * Returns a formatted date string for the expiration time of the punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return A formatted expiration date string, or {@code null} if no expiration time is set.
     */
    String getExpiredDate(UUID id);

    /**
     * Updates the expiration time for a punishment log.
     *
     * @param id        The UUID of the punishment log record.
     * @param duration  The duration in milliseconds until expiration, or -1 to clear expiration.
     * @param updatedBy The ID of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setExpiredAt(UUID id, long duration, int updatedBy);

    /**
     * Checks whether the punishment log record is expired.
     *
     * @param id The UUID of the punishment log record.
     * @return {@code true} if the record is expired; {@code false} otherwise.
     */
    boolean isExpired(UUID id);

    /**
     * Retrieves the user ID of the creator of the punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return The creator's user ID, or -2 if not found.
     */
    int getCreatedBy(UUID id);

    /**
     * Retrieves the creation timestamp of the punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return The creation Timestamp, or {@code null} if not found.
     */
    Timestamp getCreatedAt(UUID id);

    /**
     * Returns a formatted creation date string for the punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return A formatted creation date string, or {@code null} if unavailable.
     */
    String getCreatedDate(UUID id);

    /**
     * Retrieves the user ID of the last updater of the punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return The updater's user ID, or -2 if not found.
     */
    int getUpdatedBy(UUID id);

    /**
     * Retrieves the last update timestamp of the punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return The update Timestamp, or {@code null} if not found.
     */
    Timestamp getUpdatedAt(UUID id);

    /**
     * Returns a formatted update date string for the punishment log.
     *
     * @param id The UUID of the punishment log record.
     * @return A formatted update date string, or {@code null} if the update timestamp is unavailable.
     */
    String getUpdatedDate(UUID id);
}
