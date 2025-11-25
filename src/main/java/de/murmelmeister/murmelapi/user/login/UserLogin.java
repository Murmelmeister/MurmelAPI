package de.murmelmeister.murmelapi.user.login;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a user login record, including login and logout times, IP address, client brand, and protocol version.
 * This record is immutable and provides a concise way to store user login information.
 *
 * @param id              Unique identifier for the login record
 * @param userId          ID of the user who logged in
 * @param loginTime       Timestamp of when the user logged in
 * @param logoutTime      Timestamp of when the user logged out (nullable)
 * @param ipAddress       IP address from which the user logged in
 * @param clientBrand     Brand of the client used for login
 * @param protocolVersion Version of the protocol used for login
 */
public record UserLogin(UUID id, int userId, LocalDateTime loginTime, LocalDateTime logoutTime, String ipAddress,
                        String clientBrand, int protocolVersion) {
}
