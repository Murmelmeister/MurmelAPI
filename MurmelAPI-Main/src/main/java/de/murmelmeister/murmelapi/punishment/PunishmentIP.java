package de.murmelmeister.murmelapi.punishment;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Represents an IP address that has been punished or is subject to a punishment.
 */
public sealed interface PunishmentIP permits PunishmentIPProvider {
    /**
     * Checks if a punishment record exists for the given type ID and IP address.
     *
     * @param inetAddress The IP address to check for existence of a punishment
     * @param typeId      The ID representing the type of punishment
     * @return {@code true} if a punishment record exists for the specified type ID and IP address, {@code false} otherwise
     */
    boolean exists(InetAddress inetAddress, int typeId);

    /**
     * Applies a punishment for a given IP address with specific type and reason.
     *
     * @param inetAddress The IP address to which the punishment will be applied
     * @param typeId      The type identifier of the punishment
     * @param executorId  The identifier of the executor applying the punishment
     * @param reasonId    The identifier of the reason for the punishment
     */
    void punish(InetAddress inetAddress, int typeId, int executorId, int reasonId);

    /**
     * Removes the punishment associated with the given type and IP address.
     *
     * @param inetAddress The IP address for which the punishment is removed
     * @param typeId      The identifier of the punishment type to remove
     */
    void unpunished(InetAddress inetAddress, int typeId);

    /**
     * Retrieves a list of IP addresses associated with the specified type.
     *
     * @param typeId The identifier for the type of punishment or record for which the IPs are to be retrieved
     * @return A list of IP addresses corresponding to the specified type
     */
    List<String> getIps(int typeId);

    /**
     * Retrieves the log identifier associated with the specified type ID and IP address.
     *
     * @param inetAddress The IP address for which the log ID is being queried
     * @param typeId      An integer representing the type identifier for the punishment or operation
     * @return A UUID representing the log identifier, or null if no log is associated with the given parameters
     */
    UUID getLogId(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the reason identifier associated with a specific type and IP address.
     *
     * @param inetAddress The IP address for which the reason identifier is being requested.
     * @param typeId      The identifier representing the punishment type.
     * @return The reason identifier associated with the specified type and IP address.
     */
    int getReasonId(InetAddress inetAddress, int typeId);

    /**
     * Updates the reason ID for an existing punishment associated with the specified type, IP address, and executor.
     *
     * @param inetAddress The IP address associated with the punishment
     * @param typeId      The type identifier for the punishment
     * @param executorId  The identifier of the executor making the change
     * @param reasonId    The new reason identifier to be set
     */
    void setReasonId(InetAddress inetAddress, int typeId, int executorId, int reasonId);

    /**
     * Retrieves the reason associated with a specific punishment type and IP address.
     *
     * @param inetAddress The IP address associated with the punishment
     * @param typeId      The identifier for the punishment type
     * @return The reason for the punishment as a String
     */
    String getReason(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the duration associated with a specific type and IP address.
     *
     * @param inetAddress The IP address associated with the entity
     * @param typeId      The identifier of the type for which the duration is being retrieved
     * @return The duration in milliseconds
     */
    long getDuration(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the expiration time of the punishment for the specified type and IP address.
     *
     * @param inetAddress The IP address associated with the punishment
     * @param typeId      The identification number of the punishment type
     * @return The expiration time of the punishment in milliseconds since epoch
     */
    long getExpiredTime(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the expiration date as a string for a specific punishment type and IP address.
     *
     * @param inetAddress The IP address associated with the punishment
     * @param typeId      The identifier of the punishment type
     * @return The expiration date as a string, or null if no expiration date is set
     */
    String getExpiredDate(InetAddress inetAddress, int typeId);

    /**
     * Sets the expiration time for a specific punishment on an IP address.
     *
     * @param inetAddress The IP address associated with the punishment.
     * @param typeId      The type identifier of the punishment.
     * @param executorId  The ID of the executor making this change.
     * @param time        The new expiration time for the punishment in milliseconds since epoch.
     */
    void setExpiredTime(InetAddress inetAddress, int typeId, int executorId, long time);

    /**
     * Checks if a specific IP address is currently punished for a given type.
     *
     * @param inetAddress The IP address to check
     * @param typeId      The unique identifier of the type of punishment
     * @return true if the IP address is punished for the specified type, false otherwise
     */
    boolean isPunished(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the identifier of the user who created a punishment for the specified type and IP address.
     *
     * @param inetAddress The IP address associated with the punishment
     * @param typeId      The type identifier associated with the punishment
     * @return The identifier of the creator of the punishment
     */
    int getCreatedBy(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the timestamp indicating when the specified punishment record was created.
     *
     * @param inetAddress The IP address associated with the punishment record
     * @param typeId      The unique identifier of the punishment type
     * @return The timestamp of the creation time for the punishment record
     */
    Timestamp getCreatedAt(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the ID of the entity or executor that last modified the punishment record
     * based on the provided punishment type ID and IP address.
     *
     * @param inetAddress The IP address associated with the punishment.
     * @param typeId      The type identifier of the punishment.
     * @return The identifier of the entity or executor who last modified the punishment record.
     */
    int getModifiedBy(InetAddress inetAddress, int typeId);

    /**
     * Retrieves the timestamp when the specified punishment was last modified.
     *
     * @param inetAddress The IP address associated with the punishment
     * @param typeId      The identifier of the punishment type
     * @return The timestamp indicating the last modification time of the punishment
     */
    Timestamp getModifiedAt(InetAddress inetAddress, int typeId);
}
