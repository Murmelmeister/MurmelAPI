package de.murmelmeister.murmelapi.user.session;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionProvider {
    void refreshCache();

    @NotNull Optional<UserSession> findById(@NotNull UUID sessionId);

    @NotNull Optional<UserSession> findByUserId(int userId);

    @NotNull @Unmodifiable
    List<UserSession> findAll();

    @NotNull Optional<UserSession> create(int userId, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion);

    int delete(@NotNull UUID sessionId);

    @ApiStatus.Internal
    static @NotNull UserSessionProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new UserSessionProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
