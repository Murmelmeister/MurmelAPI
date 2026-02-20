package de.murmelmeister.murmelapi.user.login;

import de.murmelmeister.murmelapi.user.session.UserSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserLoginProvider {
    void refreshCache();

    @Nullable UserLogin findById(@Nullable UUID id);

    @NotNull
    @Unmodifiable
    List<UserLogin> findByUserId(int userId);

    @NotNull
    @Unmodifiable
    List<UserLogin> findByIpAddress(@Nullable InetAddress inetAddress);

    @NotNull
    @Unmodifiable
    List<UserLogin> findAll();

    @Nullable UserLogin create(@NotNull UUID sessionId, int userId, @NotNull LocalDateTime loginTime, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion);

    @Nullable UserLogin create(@NotNull UserSession session);

    int delete(@Nullable UUID id);
}
