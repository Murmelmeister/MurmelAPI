package de.murmelmeister.murmelapi.group.permission;

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

public class GroupPermissionCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupPermissionCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP_ID = "SELECT * FROM %s WHERE group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE group_id = ? AND permission = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PermissionKey, Optional<GroupPermission>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<GroupPermission>> cacheByGroupId;
    private final LoadingCache<@NotNull String, List<GroupPermission>> listCache;

    public GroupPermissionCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByGroupId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.GROUP_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_GROUP_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof PermissionKey permissionKey)
                remove(permissionKey);
            else if (key instanceof String json) {
                final Gson gson = new Gson();
                try {
                    final PermissionKey permissionKey = gson.fromJson(json, PermissionKey.class);
                    remove(permissionKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single group permission refresh: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<GroupPermission> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupPermission());
    }

    private @NotNull List<GroupPermission> loadByUserId(int groupId) {
        String sql = SELECT_BY_GROUP_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupPermission(),
                stmt -> stmt.setInt(1, groupId));
    }

    private @NotNull Optional<GroupPermission> loadByKey(PermissionKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        GroupPermission groupPermission = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.groupPermission(),
                stmt -> {
                    stmt.setInt(1, key.groupId());
                    if (key.permission() != null) stmt.setString(2, key.permission());
                    else stmt.setNull(2, Types.VARCHAR);
                });

        return Optional.ofNullable(groupPermission);
    }

    public @Nullable GroupPermission get(int groupId, @NotNull String permission) {
        Optional<GroupPermission> optPermission = cacheByKey.get(new PermissionKey(groupId, permission));
        return optPermission != null && optPermission.isPresent() ? optPermission.orElse(null) : null;
    }

    public @Nullable List<GroupPermission> getPermissions(int groupId) {
        return cacheByGroupId.get(groupId);
    }

    public void remove(@NotNull PermissionKey key) {
        cacheByKey.invalidate(key);
        if (key.permission() == null) {
            cacheByGroupId.invalidate(key.groupId());
            CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == key.groupId());
        } else {
            CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == key.groupId() && v.permission().equals(key.permission()));
            CacheUtil.remove(cacheByGroupId, key.groupId(), v -> v.groupId() == key.groupId() && v.permission().equals(key.permission()));
        }
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByGroupId.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<GroupPermission> getCachedPermissions() {
        List<GroupPermission> permissions = listCache.get(ALL_KEY);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    public record PermissionKey(int groupId, @Nullable String permission) {
    }
}
