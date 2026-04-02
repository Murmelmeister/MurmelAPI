package de.murmelmeister.murmelapi.user.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

public interface UserSession {
    @NotNull UUID id();

    int userId();

    @NotNull LocalDateTime loginTime();

    @NotNull InetAddress inetAddress();

    @Nullable String clientBrand();

    int protocolVersion();

    static UserSession of(@NotNull UUID id, int userId, @NotNull LocalDateTime loginTime, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion) {
        return new UserSessionImpl(id, userId, loginTime, inetAddress, clientBrand, protocolVersion);
    }
}

