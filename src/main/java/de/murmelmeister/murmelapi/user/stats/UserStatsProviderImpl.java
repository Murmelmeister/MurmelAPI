package de.murmelmeister.murmelapi.user.stats;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;

final class UserStatsProviderImpl implements UserStatsProvider {
    private final RefreshProvider refreshProvider;
    private final UserStatsCache cache;
    private final RefreshType all = RefreshType.USER_STATS;
    private final RefreshType single = RefreshType.SINGLE_USER_STAT;

    public UserStatsProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.refreshProvider = refreshProvider;
        this.cache = new UserStatsCache(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public void refreshSingle(int userId) {
        refreshProvider.fireSingle(single, new UserStatsCache.StatsKey(userId));
    }

    @Override
    public @NotNull Optional<UserStats> findByUserId(int userId) {
        return cache.getById(userId);
    }
}
