package de.murmelmeister.murmelapi.user.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record UserSession(
        @NotNull UUID id,
        int userId,
        @NotNull LocalDateTime loginTime,
        @NotNull InetAddress inetAddress,
        @Nullable String clientBrand,
        int protocolVersion
) {
    public UserSession {
        Objects.requireNonNull(id, "Session ID cannot be null");
        Objects.requireNonNull(loginTime, "Login time cannot be null");
        Objects.requireNonNull(inetAddress, "IP address cannot be null");
        if (clientBrand != null && clientBrand.length() > 50)
            throw new IllegalArgumentException("Client brand cannot be longer than 50 characters");
    }
}

