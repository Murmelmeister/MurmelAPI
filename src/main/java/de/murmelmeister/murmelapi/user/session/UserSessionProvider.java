package de.murmelmeister.murmelapi.user.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface UserSessionProvider {
    void refreshCache();

    @Nullable UserSession findById(@Nullable UUID sessionId);

    @Nullable UserSession findByUserId(int userId);

    @NotNull @Unmodifiable
    List<UserSession> findAll();

    @Nullable UserSession create(int userId, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion);

    int delete(@NotNull UUID sessionId);
}
