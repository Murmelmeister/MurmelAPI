package de.murmelmeister.murmelapi.punishment.reason;

import java.sql.Timestamp;
import java.util.List;

/**
 * Represents a punishment reason in the system.
 * <p>
 * This interface provides methods to manage punishment reasons, including adding, removing,
 * updating, and retrieving reasons and their associated metadata.
 */
public sealed interface PunishmentReason permits PunishmentReasonProvider {
    /**
     * Checks whether a punishment reason exists for the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return {@code true} if a record with the given id exists; {@code false} otherwise.
     */
    boolean existsReason(int id);

    /**
     * Adds a new punishment reason record to the database.
     *
     * @param id         The unique identifier for the punishment reason.
     * @param typeId     The type id referencing the punishment type.
     * @param reason     The punishment reason text.
     * @param duration   The duration (in milliseconds) associated with the punishment.
     * @param autoFlagIp {@code true} if the system should automatically flag IPs for this reason; {@code false} otherwise.
     * @param autoPunish {@code true} if the system should automatically apply punishment; {@code false} otherwise.
     * @param createdBy  The id of the user creating the punishment reason.
     * @return The number of rows affected by the insertion, or 0 if input parameters are invalid.
     */
    int addReason(int id, int typeId, String reason, long duration, boolean autoFlagIp, boolean autoPunish, int createdBy);

    /**
     * Removes the punishment reason record with the specified id.
     *
     * @param id The unique identifier of the punishment reason to remove.
     * @return The number of rows affected by the deletion.
     */
    int removeReason(int id);

    /**
     * Retrieves a list of all punishment reason ids from the database.
     *
     * @return A list of punishment reason ids.
     */
    List<Integer> getReasonIds();

    /**
     * Retrieves the punishment reason text for the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return The punishment reason as a String, or {@code null} if not found.
     */
    String getReason(int id);

    /**
     * Updates the punishment reason text for the specified id.
     *
     * @param id        The unique identifier of the punishment reason.
     * @param reason    The new punishment reason text.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setReason(int id, String reason, int updatedBy);

    /**
     * Retrieves the duration associated with the punishment reason for the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return The duration as a long, or -1 if not found.
     */
    long getDuration(int id);

    /**
     * Updates the duration for the punishment reason with the specified id.
     *
     * @param id        The unique identifier of the punishment reason.
     * @param duration  The new duration in milliseconds.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setDuration(int id, long duration, int updatedBy);

    /**
     * Retrieves the auto-flag IP flag for the punishment reason with the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return {@code true} if auto-flag IP is enabled; {@code false} otherwise.
     */
    boolean getAutoFlagIp(int id);

    /**
     * Updates the auto-flag IP flag for the punishment reason with the specified id.
     *
     * @param id         The unique identifier of the punishment reason.
     * @param autoFlagIp The new value for the auto-flag IP flag.
     * @param updatedBy  The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setAutoFlagIp(int id, boolean autoFlagIp, int updatedBy);

    /**
     * Retrieves the auto-punish flag for the punishment reason with the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return {@code true} if auto-punish is enabled; {@code false} otherwise.
     */
    boolean getAutoPunish(int id);

    /**
     * Updates the auto-punish flag for the punishment reason with the specified id.
     *
     * @param id         The unique identifier of the punishment reason.
     * @param autoPunish The new value for the auto-punish flag.
     * @param updatedBy  The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setAutoPunish(int id, boolean autoPunish, int updatedBy);

    /**
     * Retrieves the user ID of the creator of the punishment reason with the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return The creator's user ID, or -2 if not found.
     */
    int getCreatedBy(int id);

    /**
     * Retrieves the creation timestamp of the punishment reason with the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return The creation timestamp, or {@code null} if not found.
     */
    Timestamp getCreatedAt(int id);

    /**
     * Returns a formatted date string representing the creation date of the punishment reason.
     *
     * @param id The unique identifier of the punishment reason.
     * @return The formatted creation date string, or {@code null} if the creation timestamp is unavailable.
     */
    String getCreatedDate(int id);

    /**
     * Retrieves the user ID of the last updater of the punishment reason with the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return The updater's user ID, or -2 if not found.
     */
    int getUpdatedBy(int id);

    /**
     * Retrieves the last update timestamp of the punishment reason with the specified id.
     *
     * @param id The unique identifier of the punishment reason.
     * @return The last update timestamp, or {@code null} if not found.
     */
    Timestamp getUpdatedAt(int id);

    /**
     * Returns a formatted date string representing the last update date of the punishment reason.
     *
     * @param id The unique identifier of the punishment reason.
     * @return The formatted update date string, or {@code null} if the update timestamp is unavailable.
     */
    String getUpdatedDate(int id);
}
