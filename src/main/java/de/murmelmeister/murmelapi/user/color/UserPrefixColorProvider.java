package de.murmelmeister.murmelapi.user.color;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface UserPrefixColorProvider {
    void refreshCache();

    @NotNull Optional<UserPrefixColor> findById(int userId, @NotNull String colorId);

    @NotNull Optional<UserPrefixColor> findActiveById(int userId);

    @NotNull
    @Unmodifiable
    List<UserPrefixColor> findByUserId(int userId);

    @NotNull Optional<UserPrefixColor> upsert(int userId, @NotNull String colorId, boolean active);

    int delete(int userId, @NotNull String colorId);

    @ApiStatus.Internal
    static @NotNull UserPrefixColorProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new UserPrefixColorProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
