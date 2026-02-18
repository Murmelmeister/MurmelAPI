package de.murmelmeister.murmelapi.user.stats;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

public class UserStatsCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserStatsCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE id = ?";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<UserStats>> cacheById;

    public UserStatsCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
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
            if (key instanceof UserStats userStats)
                remove(userStats);
            else if (key instanceof String json) {
                try {
                    final UserStats userStats = gson.fromJson(json, UserStats.class);

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
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        UserStats userStats = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userStats(),
                stmt -> stmt.setInt(1, userId));

        return Optional.ofNullable(userStats);
    }

    public @Nullable UserStats getById(int userId) {
        Optional<UserStats> optStats = cacheById.get(userId);
        return optStats != null && optStats.isPresent() ? optStats.orElse(null) : null;
    }

    public void remove(@NotNull UserStats userStats) {
        cacheById.invalidate(userStats.userId());
    }

    public void clear() {
        cacheById.invalidateAll();
    }
}
