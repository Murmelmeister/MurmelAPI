package de.murmelmeister.murmelapi.group.permission;

import com.github.benmanes.caffeine.cache.LoadingCache;
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

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupPermissionCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP_ID = "SELECT * FROM %s WHERE group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE group_id = ? AND permission = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*groupId=(\\d+), permission=([^,\\]]+).*");

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
            if (!(key instanceof String)) {
                if (key instanceof PermissionKey(int groupId, String permission))
                    remove(groupId, permission);
                else if (key instanceof Integer groupId)
                    remove(groupId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int groupId = Integer.parseInt(matcher.group(1));
                    String permission = matcher.group(2);
                    remove(groupId, permission);
                } else {
                    int groupId = Integer.parseInt((String) key);
                    remove(groupId);
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
                    stmt.setString(2, key.permission());
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

    public void put(@Nullable GroupPermission groupPermission) {
        if (groupPermission == null) return;
        PermissionKey key = new PermissionKey(groupPermission.groupId(), groupPermission.permission());
        cacheByKey.put(key, Optional.of(groupPermission));
        CacheUtil.put(cacheByGroupId, groupPermission.groupId(), groupPermission,
                v -> v.groupId() == groupPermission.groupId() && v.permission().equals(groupPermission.permission()));
        CacheUtil.put(listCache, ALL_KEY, groupPermission,
                v -> v.groupId() == groupPermission.groupId() && v.permission().equals(groupPermission.permission()));
    }

    public void remove(int groupId, @NotNull String permission) {
        PermissionKey key = new PermissionKey(groupId, permission);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByGroupId, groupId,
                v -> v.groupId() == groupId && v.permission().equals(permission));
        CacheUtil.remove(listCache, ALL_KEY,
                v -> v.groupId() == groupId && v.permission().equals(permission));
    }

    public void remove(int groupId) {
        cacheByKey.asMap().keySet().stream().filter(key -> key.groupId() == groupId)
                .forEach(cacheByKey::invalidate);
        cacheByGroupId.invalidate(groupId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == groupId);
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

    protected record PermissionKey(int groupId, @NotNull String permission) {
    }
}
