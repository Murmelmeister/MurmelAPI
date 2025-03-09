package de.murmelmeister.murmelapi.logging;

import java.net.InetAddress;
import java.security.Timestamp;
import java.util.UUID;

/**
 * Represents a collection of active sessions for users.
 */
public sealed interface ActiveSession permits ActiveSessionProvider {
    /**
     * Checks whether a session exists for the specified user ID.
     *
     * @param userId The unique identifier of the user whose session existence is to be checked.
     * @return true if a session exists for the specified user ID, false otherwise.
     */
    boolean existsSession(int userId);

    /**
     * Creates a new session for the specified user with the provided details.
     *
     * @param userId          The unique identifier of the user for whom the session is being created.
     * @param inetAddress     The IP address of the user's device initiating the session.
     * @param clientVersion   The version of the client used by the user.
     * @param protocolVersion The protocol version used by the client to communicate.
     */
    void startSession(int userId, InetAddress inetAddress, String clientVersion, String protocolVersion);

    /**
     * Closes the active session associated with the specified user ID.
     * And create a new login entry in the login history.
     *
     * @param userId The unique identifier of the user whose session is to be closed.
     */
    void closeSession(int userId);

    /**
     * Retrieves the session ID associated with the specified user.
     *
     * @param userId The unique identifier of the user whose session ID is to be retrieved.
     * @return The UUID representing the session ID for the specified user, or null if no session exists.
     */
    UUID getSessionId(int userId);

    /**
     * Retrieves the current IP address associated with a specific user.
     *
     * @param userId The unique identifier of the user whose current IP address is being retrieved.
     * @return The current IP address as a {@code String} for the specified user,
     * or {@code null} if no IP address is found.
     */
    String getIpAddress(int userId);

    /**
     * Retrieves the timestamp of the user's login time for the active session.
     *
     * @param userId The unique identifier of the user whose login time is to be retrieved.
     * @return A {@code Timestamp} object representing the user's login time, or
     * {@code null} if no active session exists for the specified user.
     */
    Timestamp getLoginTime(int userId);

    /**
     * Retrieves the client version associated with the specified user ID.
     *
     * @param userId The unique identifier of the user whose client version is to be retrieved.
     * @return The client version as a string, or null if no client version is associated with the given user ID.
     */
    String getClientVersion(int userId);

    /**
     * Retrieves the protocol version associated with a specific user based on their user ID.
     *
     * @param userId The unique identifier of the user.
     * @return The protocol version as a string, or null if no protocol version is associated with the user.
     */
    String getProtocolVersion(int userId);

    /**
     * Checks if the user with the specified user ID is currently online.
     *
     * @param userId The unique identifier of the user to check.
     * @return true if the user is online, false otherwise.
     */
    boolean isOnline(int userId);
}
