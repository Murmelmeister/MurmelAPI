package de.murmelmeister.murmelapi.bansystem.ban;

import de.murmelmeister.murmelapi.bansystem.log.Log;
import de.murmelmeister.murmelapi.bansystem.reason.Reason;

/**
 * The Ban interface defines methods to manage user bans within a system.
 */
public sealed interface Ban permits BanProvider {
    /**
     * Bans a user for a specified time period.
     *
     * @param userId    The ID of the user to be banned.
     * @param creatorId The ID of the user who is performing the ban action.
     * @param reasonId  The ID of the reason for banning the user.
     * @param time      The duration of the ban in milliseconds since the epoch.
     */
    void ban(int userId, int creatorId, int reasonId, long time);

    /**
     * Unbans a user with the given user ID.
     *
     * @param userId The ID of the user to be unbanned.
     */
    void unban(int userId);

    /**
     * Retrieves the expiration time for the ban of the user with the specified ID.
     *
     * @param userId The ID of the user whose ban expiration time is to be retrieved.
     * @return The expiration time of the ban in milliseconds since the epoch, or -1 if the user is not banned.
     */
    long getExpiredTime(int userId);

    /**
     * Retrieves the expiration date for the ban of the user with the specified ID in a human-readable format.
     *
     * @param userId The ID of the user whose ban expiration date is to be retrieved.
     * @return The expiration date of the ban as a string, or "Not Banned" if the user is not banned.
     */
    String getExpiredDate(int userId);

    /**
     * Checks whether a user is currently banned.
     *
     * @param userId The ID of the user to check.
     * @return True if the user is banned, false otherwise.
     */
    boolean isBanned(int userId);

    /**
     * Retrieves the Reason instance associated with the Ban system.
     *
     * @return The Reason instance configured for this Ban system.
     */
    Reason getReason();

    /**
     * Retrieves the Log instance associated with this Ban system.
     *
     * @return The Log instance configured for this Ban system.
     */
    Log getLog();
}
