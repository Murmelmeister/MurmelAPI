package de.murmelmeister.murmelapi.permission;

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
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PermissionCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_USER_AND_PERMISSION = "SELECT * FROM %s WHERE user_id = ? AND permission = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP_AND_PERMISSION = "SELECT * FROM %s WHERE group_id = ? AND permission = ?";

    @Language("MariaDB")
    private static final String SELECT_BY_USER = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP = "SELECT * FROM %s WHERE group_id = ?";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PermissionKey, Optional<Permission>> cacheByKey;
    private final LoadingCache<@NotNull PermissionTarget, List<Permission>> cacheByTarget;

    public PermissionCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByTarget = CacheUtil.buildCacheRefresh(this::loadByTarget, cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
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

    private @NotNull Optional<Permission> loadByKey(@NotNull PermissionKey key) {
        String sql;
        if (key.target().type() == PermissionTarget.TargetType.USER)
            sql = SELECT_BY_USER_AND_PERMISSION.formatted(tableName);
        else sql = SELECT_BY_GROUP_AND_PERMISSION.formatted(tableName);

        Permission permission = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.permission(), stmt -> {
            stmt.setInt(1, key.target().id());
            stmt.setString(2, key.permission());
        });

        return Optional.ofNullable(permission);
    }

    private @NotNull List<Permission> loadByTarget(@NotNull PermissionTarget target) {
        String sql;
        if (target.type() == PermissionTarget.TargetType.USER)
            sql = SELECT_BY_USER.formatted(tableName);
        else sql = SELECT_BY_GROUP.formatted(tableName);

        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.permission(), stmt -> stmt.setInt(1, target.id()));
    }

    public @NotNull Optional<Permission> getByKey(@NotNull PermissionTarget target, @NotNull String permission) {
        return cacheByKey.get(new PermissionKey(target, permission));
    }

    public @NotNull @Unmodifiable List<Permission> getByTarget(@NotNull PermissionTarget target) {
        List<Permission> permissions = cacheByTarget.get(target);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    public void remove(@NotNull PermissionKey key) {
        cacheByKey.invalidate(key);
        cacheByTarget.invalidate(key.target());
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByTarget.invalidateAll();
    }

    public record PermissionKey(@NotNull PermissionTarget target, @Nullable String permission) {
    }
}
