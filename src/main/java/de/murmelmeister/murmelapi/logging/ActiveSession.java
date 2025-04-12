package de.murmelmeister.murmelapi.logging;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * The {@code ActiveSession} interface provides methods for managing user sessions.
 * It allows checking for existing sessions, starting and closing sessions, and retrieving session information.
 * This interface is designed to be implemented by classes that provide specific session management functionalities.
 */
public sealed interface ActiveSession permits ActiveSessionProvider {
    /**
     * Checks if an active session exists for the specified user.
     *
     * @param userId The user id to check for an active session.
     * @return {@code true} if the user id is greater than 0 and an active session exists; {@code false} otherwise.
     */
    boolean existsSession(int userId);

    /**
     * Starts a new active session for the specified user.
     * A new session record is created with a random UUID and the given session parameters.
     *
     * @param userId          The user id for which to start the session.
     * @param ipAddress       The IP address from which the session is started.
     * @param clientVersion   The client version used during the session.
     * @param protocolVersion The protocol version used during the session.
     * @return The number of rows affected by the session start operation, or 0 if userId is less than 1.
     */
    int startSession(int userId, String ipAddress, String clientVersion, String protocolVersion);

    /**
     * Starts a new active session for the specified user using an InetAddress for the IP address.
     * This method delegates to {@link #startSession(int, String, String, String)} by converting the InetAddress.
     *
     * @param userId          The user id for which to start the session.
     * @param inetAddress     The InetAddress representing the IP address.
     * @param clientVersion   The client version used during the session.
     * @param protocolVersion The protocol version used during the session.
     * @return The number of rows affected by the session start operation.
     */
    int startSession(int userId, InetAddress inetAddress, String clientVersion, String protocolVersion);

    /**
     * Closes the active session for the specified user.
     * The session is archived (e.g. inserted into the login history) and then removed from the active sessions table.
     *
     * @param userId The user id for which the session should be closed.
     */
    void closeSession(int userId);

    /**
     * Retrieves the session ID for the active session of the specified user.
     *
     * @param userId The user id whose session id is to be retrieved.
     * @return The session UUID if found; {@code null} if user id is less than 1 or no session record exists.
     */
    UUID getSessionId(int userId);

    /**
     * Retrieves the IP address associated with the active session of the specified user.
     *
     * @param userId The user id whose session IP address is to be retrieved.
     * @return The IP address as a String; {@code null} if user id is less than 1 or no session record exists.
     */
    String getIpAddress(int userId);

    /**
     * Retrieves the login time of the active session for the specified user.
     *
     * @param userId The user id whose login time is to be retrieved.
     * @return A Timestamp representing the login time; {@code null} if user id is less than 1 or no session record exists.
     */
    Timestamp getLoginTime(int userId);

    /**
     * Formats the login time of the active session for the specified user into a readable date string.
     *
     * @param userId The user id whose login date is to be formatted.
     * @return A formatted date string representing the login time, or {@code null} if no login time is available.
     */
    String getLoginDate(int userId);

    /**
     * Retrieves the client version used in the active session for the specified user.
     *
     * @param userId The user id whose session's client version is to be retrieved.
     * @return The client version as a String; {@code null} if user id is less than 1 or no session record exists.
     */
    String getClientVersion(int userId);

    /**
     * Retrieves the protocol version used in the active session for the specified user.
     *
     * @param userId The user id whose session's protocol version is to be retrieved.
     * @return The protocol version as a String; {@code null} if user id is less than 1 or no session record exists.
     */
    String getProtocolVersion(int userId);

    /**
     * Checks if the specified user is currently online (i.e. has an active session).
     *
     * @param userId The user id to check for an active session.
     * @return {@code true} if the user has an active session; {@code false} if user id is less than 1 or no active session exists.
     */
    boolean isOnline(int userId);
}
