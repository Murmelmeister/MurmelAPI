package de.murmelmeister.murmelapi.user.stats;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;

public interface UserStatsProvider {
    void refreshCache();

    void refreshSingle(int userId);

    @NotNull Optional<UserStats> findByUserId(int userId);

    @ApiStatus.Internal
    static @NotNull UserStatsProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new UserStatsProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
