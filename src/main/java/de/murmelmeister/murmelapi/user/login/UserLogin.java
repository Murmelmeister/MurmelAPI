package de.murmelmeister.murmelapi.user.login;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user login record, including login and logout times, IP address, client brand, and protocol version.
 * This record is immutable and provides a concise way to store user login information.
 *
 * @param id              Unique identifier for the login record
 * @param userId          ID of the user who logged in
 * @param loginTime       Timestamp of when the user logged in
 * @param logoutTime      Timestamp of when the user logged out (nullable)
 * @param inetAddress     IP address from which the user logged in
 * @param clientBrand     Brand of the client used for login
 * @param protocolVersion Version of the protocol used for login
 */
public record UserLogin(
        @NotNull UUID id,
        int userId,
        @NotNull LocalDateTime loginTime,
        @NotNull LocalDateTime logoutTime,
        @NotNull InetAddress inetAddress,
        @Nullable String clientBrand,
        int protocolVersion
) {
    public UserLogin {
        Objects.requireNonNull(id, "Login ID cannot be null");
        Objects.requireNonNull(loginTime, "Login time cannot be null");
        Objects.requireNonNull(logoutTime, "Logout time cannot be null");
        Objects.requireNonNull(inetAddress, "IP address cannot be null");
    }
}
