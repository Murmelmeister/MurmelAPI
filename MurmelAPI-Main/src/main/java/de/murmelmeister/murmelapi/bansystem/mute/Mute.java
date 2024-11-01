package de.murmelmeister.murmelapi.bansystem.mute;


import de.murmelmeister.murmelapi.bansystem.log.Log;
import de.murmelmeister.murmelapi.bansystem.reason.Reason;

/**
 * The Mute interface represents operations related to muting and unmuting users
 * in a system, including actions that define the duration and reason for the mute.
 */
public sealed interface Mute permits MuteProvider {
    /**
     * Mutes a user for a specified period of time.
     *
     * @param userId The ID of the user to be muted.
     * @param creatorId The ID of the user who initiates the mute action.
     * @param reasonId The ID of the reason for the mute.
     * @param time The duration of the mute in milliseconds from the current time.
     */
    void mute(int userId, int creatorId, int reasonId, long time);

    /**
     * Unmutes a user, removing any mute restrictions previously placed on them.
     *
     * @param userId The ID of the user to be unmuted.
     */
    void unmute(int userId);

    /**
     * Retrieves the expiration time of the mute for the specified user.
     *
     * @param userId The ID of the user whose mute expiration time is to be retrieved.
     * @return The expiration time of the mute in milliseconds since epoch, or -1 if the user is not muted.
     */
    long getExpiredTime(int userId);

    /**
     * Retrieves the expiration date of the mute for the specified user.
     *
     * @param userId The ID of the user whose mute expiration date is to be retrieved.
     * @return The expiration date of the mute as a string.
     */
    String getExpiredDate(int userId);

    /**
     * Checks if a user is currently muted.
     *
     * @param userId The ID of the user to be checked.
     * @return True if the user is muted, false otherwise.
     */
    boolean isMuted(int userId);

    /**
     * Provides the reason associated with the mute action.
     *
     * @return The reason for the mute, represented as a Reason object.
     */
    Reason getReason();

    /**
     * Retrieves the log associated with the mute actions.
     *
     * @return The log object that stores and retrieves entries related to mute actions.
     */
    Log getLog();
}
