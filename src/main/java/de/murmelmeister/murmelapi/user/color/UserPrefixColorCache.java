package de.murmelmeister.murmelapi.user.color;

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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class UserPrefixColorCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPrefixColorCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE user_id = ? AND color_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_USER = "SELECT * FROM %s WHERE user_id = ?";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ColorKey, Optional<UserPrefixColor>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<UserPrefixColor>> cacheByUser;

    public UserPrefixColorCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, java.time.Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByUser = CacheUtil.buildCacheRefresh(this::loadByUser, cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.USER_PREFIX_COLORS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_PREFIX_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof ColorKey colorKey)
                remove(colorKey);
            else if (key instanceof String json) {
                try {
                    final ColorKey colorKey = gson.fromJson(json, ColorKey.class);

                    if (colorKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(colorKey);
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

    private @NotNull Optional<UserPrefixColor> loadByKey(ColorKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        UserPrefixColor userPrefixColor = CacheUtil.loadSingle(database, sql, fetchLimit, UserPrefixColorRowMapper::resultSet, stmt -> {
            stmt.setInt(1, key.userId());
            stmt.setString(2, key.colorId());
        });

        return Optional.ofNullable(userPrefixColor);
    }

    private @NotNull List<UserPrefixColor> loadByUser(int userId) {
        String sql = SELECT_BY_USER.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, UserPrefixColorRowMapper::resultSet, stmt -> stmt.setInt(1, userId));
    }

    public @NotNull Optional<UserPrefixColor> getByKey(int userId, @NotNull String colorId) {
        return cacheByKey.get(new ColorKey(userId, colorId));
    }

    public @NotNull @Unmodifiable List<UserPrefixColor> getByUser(int userId) {
        List<UserPrefixColor> colors = cacheByUser.get(userId);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return List.copyOf(colors);
    }

    public void remove(@NotNull ColorKey key) {
        cacheByKey.invalidate(key);
        cacheByUser.invalidate(key.userId());
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByUser.invalidateAll();
    }

    public record ColorKey(int userId, @Nullable String colorId) {
    }
}
