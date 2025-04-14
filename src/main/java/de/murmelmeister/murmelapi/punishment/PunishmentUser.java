package de.murmelmeister.murmelapi.punishment;

import java.util.UUID;

/**
 * PunishmentUser interface to manage user punishments in the database.
 * This interface provides methods to check for user existence, punish and unpunish users, and retrieve punishment logs.
 */
public sealed interface PunishmentUser permits PunishmentUserProvider {
    /**
     * Checks whether a punishment record exists for the specified user.
     *
     * @param userId The ID of the user.
     * @return {@code true} if the punishment record exists; {@code false} otherwise.
     */
    boolean exists(int userId);

    /**
     * Creates a punishment record for the specified user.
     * This method also creates a punishment log entry.
     *
     * @param userId    The ID of the user to be punished.
     * @param ipAddress The IP address associated with the punishment.
     * @param reasonId  The identifier of the punishment reason.
     * @param createdBy The ID of the user applying the punishment.
     * @return The number of rows affected by the insertion; returns 0 if the userId is less than 1.
     */
    int punish(int userId, String ipAddress, int reasonId, int createdBy);

    /**
     * Removes the punishment record for the specified user.
     *
     * @param userId The ID of the punished user.
     * @return The number of rows affected by the removal.
     */
    int unpunish(int userId);

    /**
     * Retrieves the punishment log ID for the specified user.
     *
     * @param userId The ID of the user.
     * @return The UUID of the punishment log, or {@code null} if no log is found.
     */
    UUID getLogId(int userId);

    /**
     * Checks whether the specified user is currently punished.
     * The method retrieves the punishment log for the user and verifies that the punishment is not expired.
     *
     * @param userId The ID of the user.
     * @return {@code true} if a punishment log exists and is not expired; {@code false} otherwise.
     */
    boolean isPunished(int userId);
}
