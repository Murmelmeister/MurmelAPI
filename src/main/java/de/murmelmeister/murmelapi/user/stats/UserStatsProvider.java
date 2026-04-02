package de.murmelmeister.murmelapi.user.stats;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserStatsProvider {
    void refreshCache();

    @NotNull Optional<UserStats> findByUserId(int userId);

    @NotNull Optional<UserStats> create(int userId);

    int delete(int userId);

    @NotNull Optional<UserStats> update(int userId, int playTime, int dailyStreak, @Nullable LocalDate lastDay, @Nullable LocalDateTime lastSeen);

    @ApiStatus.Internal
    static @NotNull UserStatsProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new UserStatsProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
