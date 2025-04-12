package de.murmelmeister.murmelapi.logging;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * The {@code LoginHistory} interface provides methods for managing login history data in a database.
 * It allows checking for login existence, deleting user logins, and retrieving login information.
 * This interface is designed to be implemented by classes that provide specific database interactions.
 */
public sealed interface LoginHistory permits LoginHistoryProvider {
    /**
     * Checks if a login record exists for the given loginId.
     *
     * @param loginId The UUID representing the login record.
     * @return {@code true} if the loginId is not null and a corresponding record exists in the database; {@code false} otherwise.
     */
    boolean existsLogin(UUID loginId);

    /**
     * Deletes all login records associated with the specified userId.
     *
     * @param userId The user id for which the login records should be deleted.
     * @return The number of rows affected by the deletion; returns 0 if the userId is less than 1.
     */
    int deleteUserLogins(int userId);

    /**
     * Retrieves the user id associated with the specified login record.
     *
     * @param loginId The UUID of the login record.
     * @return The user id if found; returns -2 if loginId is null or the record is not found.
     */
    int getUserId(UUID loginId);

    /**
     * Retrieves the IP address associated with the specified login record.
     *
     * @param loginId The UUID of the login record.
     * @return The IP address as a String; returns {@code null} if loginId is null or no record is found.
     */
    String getIpAddress(UUID loginId);

    /**
     * Retrieves the login timestamp for the specified login record.
     *
     * @param loginId The UUID of the login record.
     * @return A Timestamp representing the login time; returns {@code null} if loginId is null or no record is found.
     */
    Timestamp getLoginTime(UUID loginId);

    /**
     * Formats the login timestamp for the specified login record into a readable date string.
     *
     * @param loginId The UUID of the login record.
     * @return A formatted date string representing the login time, or {@code null} if loginTime is not available.
     */
    String getLoginDate(UUID loginId);

    /**
     * Retrieves the logout timestamp for the specified login record.
     *
     * @param loginId The UUID of the login record.
     * @return A Timestamp representing the logout time; returns {@code null} if loginId is null or no record is found.
     */
    Timestamp getLogoutTime(UUID loginId);

    /**
     * Formats the logout timestamp for the specified login record into a readable date string.
     *
     * @param loginId The UUID of the login record.
     * @return A formatted date string representing the logout time, or {@code null} if logoutTime is not available.
     */
    String getLogoutDate(UUID loginId);

    /**
     * Retrieves the client version used during the login.
     *
     * @param loginId The UUID of the login record.
     * @return The client version as a String; returns {@code null} if loginId is null or no record is found.
     */
    String getClientVersion(UUID loginId);

    /**
     * Retrieves the protocol version used during the login.
     *
     * @param loginId The UUID of the login record.
     * @return The protocol version as a String; returns {@code null} if loginId is null or no record is found.
     */
    String getProtocolVersion(UUID loginId);

    /**
     * Retrieves a list of all login record UUIDs for the specified user.
     *
     * @param userId The user id whose login records are to be retrieved.
     * @return A List of UUIDs corresponding to the login records; returns {@code null} if userId is less than 1.
     */
    List<UUID> getUserLogins(int userId);

    /**
     * Retrieves a sorted list of login record UUIDs for the specified user, limited by the given number.
     *
     * @param userId The user id whose login records are to be retrieved.
     * @param limit  The maximum number of login records to return.
     * @return A List of UUIDs corresponding to the login records, sorted (typically by login time in descending order);
     * returns {@code null} if userId is less than 1.
     */
    List<UUID> getSortedUserLogins(int userId, int limit);

    /**
     * Retrieves the most recent login record UUID for the specified user.
     *
     * @param userId The user id whose last login record is to be retrieved.
     * @return The UUID of the last login record; returns {@code null} if userId is less than 1 or no record is found.
     */
    UUID getLastLoginId(int userId);

    /**
     * Retrieves the latest logout timestamp for the specified user.
     *
     * @param userId The user id whose last logout time is to be retrieved.
     * @return A Timestamp representing the latest logout time; returns {@code null} if userId is less than 1 or no record is found.
     */
    Timestamp getLastQuit(int userId);

    /**
     * Formats the latest logout timestamp for the specified user into a readable date string.
     *
     * @param userId The user id whose last logout time is to be formatted.
     * @return A formatted date string representing the last logout time, or {@code null} if no logout time is available.
     */
    String getLastQuitDate(int userId);

    /**
     * Retrieves a list of user ids that have logged in from the specified IP address.
     *
     * @param ipAddress The IP address to search for.
     * @return A List of user ids; returns {@code null} if ipAddress is null.
     */
    List<Integer> getMultiUserIds(String ipAddress);

    /**
     * Retrieves the time range for the login and logout events of a specified user coming from a given IP address.
     * The method returns a string representing the earliest login time and the latest logout time.
     *
     * @param userId    The user id whose login/logout time range is to be retrieved.
     * @param ipAddress The IP address to filter the login events.
     * @return A String in the format "minDate - maxDate" indicating the time range, or {@code null} if inputs are invalid.
     */
    String getTimeRange(int userId, String ipAddress);
}
