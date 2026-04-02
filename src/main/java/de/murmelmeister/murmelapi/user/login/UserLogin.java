package de.murmelmeister.murmelapi.user.login;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

public interface UserLogin {
    @NotNull UUID id();

    int userId();

    @NotNull LocalDateTime loginTime();

    @NotNull LocalDateTime logoutTime();

    @NotNull InetAddress inetAddress();

    @Nullable String clientBrand();

    int protocolVersion();

    static @NotNull UserLogin of(@NotNull UUID id, int userId, @NotNull LocalDateTime loginTime, @NotNull LocalDateTime logoutTime, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion) {
        return new UserLoginImpl(id, userId, loginTime, logoutTime, inetAddress, clientBrand, protocolVersion);
    }
}
