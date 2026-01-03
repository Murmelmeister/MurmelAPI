package de.murmelmeister.murmelapi.user.login;

import de.murmelmeister.murmelapi.user.session.UserSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserLoginProvider {
    void refreshCache();

    @Nullable UserLogin findById(@Nullable UUID id);

    @NotNull List<UserLogin> findByUserId(int userId);

    @NotNull List<UserLogin> findByIpAddress(@Nullable InetAddress inetAddress);

    @NotNull List<UserLogin> findAll();

    @Nullable UserLogin create(@NotNull UUID sessionId, int userId, @NotNull LocalDateTime loginTime, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion);

    @Nullable UserLogin create(@NotNull UserSession session);

    int delete(@Nullable UUID id);
}
