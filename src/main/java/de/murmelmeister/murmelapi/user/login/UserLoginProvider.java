package de.murmelmeister.murmelapi.user.login;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserLoginProvider {
    void refreshCache();

    void refreshSingle(@NotNull UUID id, int userId, @NotNull InetAddress inetAddress);

    @NotNull Optional<UserLogin> findById(@NotNull UUID id);

    @NotNull
    @Unmodifiable
    List<UserLogin> findByUserId(int userId);

    @NotNull
    @Unmodifiable
    List<UserLogin> findByIpAddress(@Nullable InetAddress inetAddress);

    @NotNull
    @Unmodifiable
    List<UserLogin> findAll();

    @NotNull Optional<UserLogin> create(int userId, @NotNull LocalDateTime loginTime, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion);

    @NotNull Optional<UserLogin> create(@NotNull UserSession session);

    @ApiStatus.Internal
    @NotNull Optional<UserLogin> endSession(int userId);

    int delete(@NotNull UUID id);

    @ApiStatus.Internal
    static @NotNull UserLoginProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new UserLoginProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
