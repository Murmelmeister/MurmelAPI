package de.murmelmeister.murmelapi.user.color;

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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UserPrefixColorCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPrefixColorCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE user_id = ? AND color_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ColorKey, Optional<UserPrefixColor>> cache;
    private final LoadingCache<@NotNull String, List<UserPrefixColor>> listCache;

    public UserPrefixColorCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, java.time.Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
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
                final Gson gson = new Gson();
                try {
                    final ColorKey colorKey = gson.fromJson(json, ColorKey.class);
                    remove(colorKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single user prefix color refresh: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<UserPrefixColor> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPrefixColor());
    }

    private @NotNull Optional<UserPrefixColor> loadById(ColorKey key) {
        String sql = SELECT_BY_ID.formatted(tableName);
        UserPrefixColor userPrefixColor = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userPrefixColor(), stmt -> {
            stmt.setInt(1, key.userId());
            stmt.setString(2, key.colorId());
        });

        return Optional.ofNullable(userPrefixColor);
    }

    public @Nullable UserPrefixColor get(int userId, @NotNull String colorId) {
        Optional<UserPrefixColor> optColor = cache.get(new ColorKey(userId, colorId));
        return optColor != null && optColor.isPresent() ? optColor.orElse(null) : null;
    }

    public @NotNull List<UserPrefixColor> getAll() {
        List<UserPrefixColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return colors;
    }

    public void remove(@NotNull ColorKey key) {
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == key.userId() && v.colorId().equals(key.colorId()));
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public record ColorKey(int userId, @NotNull String colorId) {
    }
}
