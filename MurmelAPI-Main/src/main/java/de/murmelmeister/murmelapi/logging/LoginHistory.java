package de.murmelmeister.murmelapi.logging;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Interface to manage login history.
 */
public sealed interface LoginHistory permits LoginHistoryProvider {
    /**
     * Checks if a login record exists with the specified login ID.
     *
     * @param loginId The unique identifier of the login record to check.
     * @return true if a login record exists with the specified login ID, false otherwise.
     */
    boolean existsLogin(UUID loginId);

    /**
     * Deletes all login entries associated with the specified user ID.
     *
     * @param userId The unique identifier of the user whose login history should be deleted.
     */
    void deleteUserLogin(int userId);

    /**
     * Retrieves a list of login IDs (UUIDs) associated with a specific user.
     *
     * @param userId The unique identifier of the user whose login records are being retrieved
     * @return A list of UUIDs representing the login records for the specified user
     */
    List<UUID> getLogins(int userId);

    /**
     * Retrieves a list of login IDs associated with a specific user, sorted in descending order
     * of login time, with a maximum limit.
     *
     * @param userId The unique identifier of the user whose sorted login records are to be retrieved
     * @return A list of UUIDs representing the login records, sorted by login time
     */
    List<UUID> getSortedLogins(int userId);

    /**
     * Retrieves the most recent login ID for a specified user from their login history.
     *
     * @param userId The unique identifier of the user whose last login ID is to be retrieved.
     * @return The UUID representing the most recent login ID for the specified user,
     * or {@code null} if no login history exists for the user.
     */
    UUID getLastLoginId(int userId);

    /**
     * Retrieves the timestamp of the most recent logout event for the specified user.
     *
     * @param userId The unique identifier of the user whose last quit time is to be retrieved.
     * @return A {@code Timestamp} object representing the time of the user's last quit,
     * or {@code null} if no quit record exists for the specified user.
     */
    Timestamp getLastQuit(int userId);

    /**
     * Retrieves a list of user IDs associated with a specified IP address.
     *
     * @param ipAddress The IP address for which the associated user IDs are to be retrieved.
     * @return A list of integers representing the user IDs associated with the given IP address.
     */
    List<Integer> getUserIdsByIP(String ipAddress);

    /**
     * Retrieves the user ID associated with the given login ID.
     *
     * @param loginId The unique identifier of the login session
     * @return The user ID linked to the specified login ID, or a negative value if not found
     */
    int getUserId(UUID loginId);

    /**
     * Retrieves the IP address associated with a specific login session.
     *
     * @param loginId The unique identifier of the login session
     * @return The IP address as a string, or null if the login session does not exist or has no associated IP address
     */
    String getIPAddress(UUID loginId);

    /**
     * Retrieves the earliest login time for a specified user associated with a specific IP address.
     *
     * @param userId    The ID of the user whose first login time is to be retrieved.
     * @param ipAddress The IP address associated with the user's login.
     * @return A Timestamp object representing the first login time for the user with the specified IP address,
     * or null if no record is found.
     */
    Timestamp getFirstLoginTimeByUser(int userId, String ipAddress);

    /**
     * Retrieves the first login timestamp associated with a specific IP address.
     *
     * @param ipAddress The IP address for which the first login timestamp is to be retrieved
     * @return The timestamp of the first login associated with the specified IP address,
     * or {@code null} if no login records exist for the given IP address
     */
    Timestamp getFirstLoginTimeByIP(String ipAddress);

    /**
     * Retrieves the most recent login time for a user based on their user ID and IP address.
     *
     * @param userId    The unique identifier of the user whose last login time is to be retrieved.
     * @param ipAddress The IP address associated with the user's login.
     * @return A {@code Timestamp} object representing the time of the user's last login from the specified IP address,
     * or {@code null} if no login information is found.
     */
    Timestamp getLastLoginTimeByUser(int userId, String ipAddress);

    /**
     * Retrieves the last login time associated with the specified IP address.
     *
     * @param ipAddress The IP address for which the last login time is to be retrieved.
     * @return A {@link Timestamp} representing the last login time for the specified IP address,
     * or null if no such login record exists.
     */
    Timestamp getLastLoginTimeByIP(String ipAddress);

    /**
     * Retrieves the login time for a specific login ID.
     *
     * @param loginId The unique identifier associated with the login.
     * @return The timestamp indicating the login time, or null if no record is found.
     */
    Timestamp getLoginTime(UUID loginId);

    /**
     * Retrieves the logout time associated with a specific login session.
     *
     * @param loginId The unique identifier of the login session
     * @return The timestamp when the user logged out, or null if the logout time is not set
     */
    Timestamp getLogoutTime(UUID loginId);

    /**
     * Retrieves the formatted logout date associated with the given login ID.
     * If no logout time is available, the method will return "never."
     *
     * @param loginId The unique identifier of the login session.
     * @return A string representing the logout date in a formatted style,
     * or "never" if the logout time is not available.
     */
    String getLogoutDate(UUID loginId);

    /**
     * Retrieves the client version associated with the given login ID.
     *
     * @param loginId The unique identifier of the login session
     * @return The client version string if found, or null if no client version is associated with the given login ID
     */
    String getClientVersion(UUID loginId);

    /**
     * Retrieves the protocol version associated with a specific login ID.
     *
     * @param loginId The unique identifier of the login session.
     * @return The protocol version as a string, or null if not found.
     */
    String getProtocolVersion(UUID loginId);
}
