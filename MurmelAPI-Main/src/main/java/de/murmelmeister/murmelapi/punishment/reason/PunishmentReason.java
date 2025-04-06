package de.murmelmeister.murmelapi.punishment.reason;

import java.sql.Timestamp;
import java.util.List;

/**
 * Represents a punishment reason, which is a specific explanation for a punishment type.
 * Each reason is associated with a unique identifier and a type identifier.
 */
public sealed interface PunishmentReason permits PunishmentReasonProvider {
    /**
     * Checks whether a punishment reason exists for the given reason ID and type ID.
     *
     * @param reasonId The unique identifier for the reason
     * @param typeId   The unique identifier for the type
     * @return true if the punishment reason exists, false otherwise
     */
    boolean exists(int reasonId, int typeId);

    /**
     * Adds a new punishment reason with the specified details.
     *
     * @param reasonId   The unique identifier for the punishment reason.
     * @param typeId     The identifier for the type of punishment.
     * @param executorId The identifier of the executor who created the punishment reason.
     * @param reason     The description or explanation of the punishment reason.
     * @param duration   The duration of the punishment in milliseconds; use -1 for indefinite duration.
     * @param autoFlagIp Whether the system should automatically flag the IP associated with the punishment.
     * @param autoPunish Whether the system should automatically enforce the punishment.
     */
    void add(int reasonId, int typeId, int executorId, String reason, long duration, boolean autoFlagIp, boolean autoPunish);

    /**
     * Removes a punishment reason from the system based on the given reason ID and type ID.
     *
     * @param reasonId The ID of the punishment reason to be removed
     * @param typeId   The ID of the type associated with the punishment reason
     */
    void remove(int reasonId, int typeId);

    /**
     * Retrieves a list of reason IDs associated with the specified type ID.
     *
     * @param typeId The ID of the punishment type for which reason IDs should be retrieved
     * @return A list of integer reason IDs associated with the specified type ID
     */
    List<Integer> getReasons(int typeId);

    /**
     * Retrieves the reason for a specific punishment based on its reason ID and type ID.
     *
     * @param reasonId The unique identifier for the reason.
     * @param typeId   The unique identifier for the type of punishment.
     * @return The reason as a string if it exists, or null if no reason is found.
     */
    String getReason(int reasonId, int typeId);

    /**
     * Updates the punishment reason for the specified reason ID and type ID.
     *
     * @param reasonId   The unique identifier for the punishment reason.
     * @param typeId     The type identifier associated with the punishment reason.
     * @param executorId The unique identifier of the user who is executing the update.
     * @param reason     The new reason to be set for the punishment.
     */
    void setReason(int reasonId, int typeId, int executorId, String reason);

    /**
     * Retrieves the duration for the specified punishment reason and type.
     *
     * @param reasonId The unique identifier for the punishment reason
     * @param typeId   The unique identifier for the punishment type
     * @return The duration associated with the specified punishment reason and type, or -1 if not found
     */
    long getDuration(int reasonId, int typeId);

    /**
     * Sets the duration of a punishment reason.
     *
     * @param reasonId   The unique identifier for the punishment reason.
     * @param typeId     The type identifier associated with the punishment reason.
     * @param executorId The unique identifier of the executor who modifies the duration.
     * @param duration   The new duration value to be set for the punishment reason.
     */
    void setDuration(int reasonId, int typeId, int executorId, long duration);

    /**
     * Retrieves the auto-flag IP status for a specific punishment reason and type.
     *
     * @param reasonId The unique ID of the punishment reason
     * @param typeId   The type ID associated with the punishment reason
     * @return true if the auto-flag IP feature is enabled for the specified reason and type, false otherwise
     */
    boolean getAutoFlagIP(int reasonId, int typeId);

    /**
     * Updates the auto-flagging of an IP address for the given punishment reason and type.
     *
     * @param reasonId   The ID of the punishment reason to update
     * @param typeId     The type ID associated with the punishment reason
     * @param executorId The ID of the executor performing the update
     * @param autoFlagIp A boolean indicating whether to enable or disable auto-flagging of the IP
     */
    void setAutoFlagIP(int reasonId, int typeId, int executorId, boolean autoFlagIp);

    /**
     * Retrieves the automatic punishment status for a specific punishment reason and type.
     *
     * @param reasonId The unique identifier of the punishment reason
     * @param typeId   The unique identifier of the punishment type
     * @return true if automatic punishment is enabled for the given reason and type, false otherwise
     */
    boolean getAutoPunish(int reasonId, int typeId);

    /**
     * Configures the auto-punishment flag for a specific punishment reason and type.
     *
     * @param reasonId   The unique identifier for the punishment reason.
     * @param typeId     The unique identifier for the punishment type.
     * @param executorId The unique identifier for the executor who is making the change.
     * @param autoPunish A boolean value indicating whether auto-punishment should be enabled (true) or disabled (false).
     */
    void setAutoPunish(int reasonId, int typeId, int executorId, boolean autoPunish);

    /**
     * Retrieves the ID of the user who created the punishment reason.
     *
     * @param reasonId The ID of the punishment reason
     * @param typeId   The ID of the punishment type
     * @return The ID of the user who created the punishment reason, or a default value if not found
     */
    int getCreatedBy(int reasonId, int typeId);

    /**
     * Retrieves the timestamp indicating when a specific punishment reason was created.
     *
     * @param reasonId The unique identifier of the punishment reason.
     * @param typeId   The unique identifier of the punishment type.
     * @return The timestamp representing the creation time of the punishment reason,
     * or {@code null} if the reason does not exist.
     */
    Timestamp getCreatedAt(int reasonId, int typeId);

    /**
     * Retrieves the date when a specific punishment reason was created.
     *
     * @param reasonId The unique identifier of the punishment reason.
     * @param typeId   The unique identifier of the punishment type.
     * @return A string representing the date when the punishment reason was created.
     */
    String getCreatedDate(int reasonId, int typeId);

    /**
     * Retrieves the user ID of the person who last modified a specific punishment reason.
     *
     * @param reasonId The unique identifier for the punishment reason.
     * @param typeId   The unique identifier for the punishment type.
     * @return The user ID of the person who last modified the specified punishment reason, or -2 if not found.
     */
    int getModifiedBy(int reasonId, int typeId);

    /**
     * Retrieves the timestamp of the last modification for a specific punishment reason.
     *
     * @param reasonId The ID of the punishment reason
     * @param typeId   The ID of the punishment type
     * @return The timestamp representing when the punishment reason was last modified
     */
    Timestamp getModifiedAt(int reasonId, int typeId);

    /**
     * Retrieves the date when a specific punishment reason was last modified.
     *
     * @param reasonId The unique identifier of the punishment reason.
     * @param typeId   The unique identifier of the punishment type.
     * @return A string representing the date when the punishment reason was last modified.
     */
    String getModifiedDate(int reasonId, int typeId);
}
