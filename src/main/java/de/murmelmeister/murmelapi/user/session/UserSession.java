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
        Objects.requireNonNull(id, "sessionId cannot be null");
        Objects.requireNonNull(loginTime, "loginTime cannot be null");
        Objects.requireNonNull(inetAddress, "inetAddress cannot be null");
        if (clientBrand != null && clientBrand.length() > 50)
            throw new IllegalArgumentException("clientBrand cannot be longer than 50 characters");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
    }
}

