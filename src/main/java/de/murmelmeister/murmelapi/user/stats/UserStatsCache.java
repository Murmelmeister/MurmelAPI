package de.murmelmeister.murmelapi.user.stats;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

final class UserStatsCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserStatsCache.class);

    @Language("MariaDB")
    private static final String CALL_USER_STATS = "{CALL get_user_live_stats(?)}";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<UserStats>> cacheById;

    public UserStatsCache(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.USER_STATS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_STAT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof StatsKey userStats)
                remove(userStats);
            else if (key instanceof String json) {
                try {
                    final StatsKey userStats = gson.fromJson(json, StatsKey.class);

                    if (userStats == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(userStats);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull Optional<UserStats> loadById(int userId) {
        UserStats userStats = CacheUtil.loadCallSingle(database, CALL_USER_STATS, fetchLimit, UserStatsRowMapper::resultSet,
                stmt -> stmt.setInt(1, userId));

        return Optional.ofNullable(userStats);
    }

    public @NotNull Optional<UserStats> getById(int userId) {
        return cacheById.get(userId);
    }

    public void remove(@NotNull StatsKey userStats) {
        cacheById.invalidate(userStats.userId());
    }

    public void clear() {
        cacheById.invalidateAll();
    }

    record StatsKey(int userId) {
    }
}
