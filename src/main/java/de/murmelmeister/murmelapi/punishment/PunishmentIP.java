package de.murmelmeister.murmelapi.punishment;

import java.util.UUID;

/**
 * The {@code PunishmentIP} interface provides methods for managing IP address punishments in a database.
 * It allows checking for the existence of punishments, punishing and unpunishing IP addresses,
 * retrieving punishment logs, and checking if an IP address is currently punished.
 * This interface is designed to be implemented by classes that provide specific database interactions.
 */
public sealed interface PunishmentIP permits PunishmentIPProvider {
    /**
     * Checks whether a punishment record exists for the given IP address.
     *
     * @param ipAddress The IP address to check.
     * @return {@code true} if ipAddress is not null and a corresponding record exists in the database;
     * {@code false} otherwise.
     */
    boolean exists(String ipAddress);

    /**
     * Applies punishment to the specified IP address.
     * This method creates a punishment log for the IP address (using the PunishmentLog provider)
     * and stores the resulting log ID in the punishment_ip table.
     *
     * @param ipAddress The IP address to punish.
     * @param reasonId  The identifier of the punishment reason.
     * @param createdBy The ID of the user applying the punishment.
     * @return The number of rows affected by the insertion, or 0 if ipAddress is null or createdBy is invalid.
     */
    int punish(String ipAddress, int reasonId, int createdBy);

    /**
     * Removes the punishment record for the given IP address.
     *
     * @param ipAddress The IP address whose punishment record is to be removed.
     * @return The number of rows affected by the deletion, or 0 if ipAddress is null.
     */
    int unpunish(String ipAddress);

    /**
     * Retrieves the punishment log ID associated with the specified IP address.
     *
     * @param ipAddress The IP address to query.
     * @return The punishment log UUID if found; {@code null} otherwise.
     */
    UUID getLogId(String ipAddress);

    /**
     * Determines whether the specified IP address is punished.
     * This is based on the existence of a punishment log that is not expired.
     *
     * @param ipAddress The IP address to check.
     * @return {@code true} if a punishment log exists for the IP address and it is not expired; {@code false} otherwise.
     */
    boolean isPunished(String ipAddress);
}
