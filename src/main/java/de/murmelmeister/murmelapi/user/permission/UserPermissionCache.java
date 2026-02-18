package de.murmelmeister.murmelapi.user.permission;

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

import java.sql.Types;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UserPermissionCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPermissionCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE user_id = ? AND permission = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PermissionKey, Optional<UserPermission>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<UserPermission>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<UserPermission>> listCache;

    public UserPermissionCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.USER_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof PermissionKey permissionKey)
                remove(permissionKey);
            else if (key instanceof String json) {
                try {
                    final PermissionKey permissionKey = gson.fromJson(json, PermissionKey.class);

                    if (permissionKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(permissionKey);
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

    private @NotNull List<UserPermission> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPermission());
    }

    private @NotNull List<UserPermission> loadByUserId(int userId) {
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPermission(),
                stmt -> stmt.setInt(1, userId));
    }

    private @NotNull Optional<UserPermission> loadByKey(PermissionKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        UserPermission userPermission = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userPermission(),
                stmt -> {
                    stmt.setInt(1, key.userId());
                    if (key.permission() != null) stmt.setString(2, key.permission());
                    else stmt.setNull(2, Types.VARCHAR);
                });

        return Optional.ofNullable(userPermission);
    }

    public @Nullable UserPermission get(int userId, @NotNull String permission) {
        Optional<UserPermission> optPermission = cacheByKey.get(new PermissionKey(userId, permission));
        return optPermission != null && optPermission.isPresent() ? optPermission.orElse(null) : null;
    }

    public @Nullable List<UserPermission> getPermissions(int userId) {
        return cacheByUserId.get(userId);
    }

    public void remove(@NotNull PermissionKey key) {
        cacheByKey.invalidate(key);
        if (key.permission() == null) {
            cacheByUserId.invalidate(key.userId());
            CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == key.userId());
        } else {
            CacheUtil.remove(cacheByUserId, key.userId(), v -> v.userId() == key.userId() && v.permission().equals(key.permission()));
            CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == key.userId() && v.permission().equals(key.permission()));
        }
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<UserPermission> getCachedPermissions() {
        List<UserPermission> permissions = listCache.get(ALL_KEY);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    public record PermissionKey(int userId, @Nullable String permission) {
    }
}
