package de.murmelmeister.murmelapi.group.permission;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupPermissionCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*groupId=(\\d+), permission=([^,\\]]+).*");
    private final Database database;
    private final String tableName;
    private final LoadingCache<PermissionKey, GroupPermission> cacheByKey;
    private final LoadingCache<Integer, List<GroupPermission>> cacheByGroupId;
    private final LoadingCache<String, List<GroupPermission>> listCache;
    private final Long fetchLimit;

    public GroupPermissionCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByGroupId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.GROUP_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_GROUP_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof PermissionKey permissionKey)
                    refreshSingle(permissionKey);
                else if (key instanceof Integer groupId)
                    refreshSingle(groupId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int groupId = Integer.parseInt(matcher.group(1));
                    String permission = matcher.group(2);
                    refreshSingle(new PermissionKey(groupId, permission));
                } else {
                    int groupId = Integer.parseInt((String) key);
                    refreshSingle(groupId);
                }
            }
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<GroupPermission> permissions = loadAllFromDatabase();
        if (permissions.isEmpty())
            return;

        Map<Integer, List<GroupPermission>> byGroup = new HashMap<>();
        for (GroupPermission permission : permissions) {
            PermissionKey key = new PermissionKey(permission.groupId(), permission.permission());
            cacheByKey.put(key, permission);
            byGroup.computeIfAbsent(permission.groupId(), ignored -> new ArrayList<>()).add(permission);
        }

        byGroup.forEach((groupId, values) -> cacheByGroupId.put(groupId, List.copyOf(values)));
        listCache.put(ALL_KEY, List.copyOf(permissions));
    }

    private void refreshSingle(int groupId) {
        remove(groupId);
        List<GroupPermission> permissions = loadByUserId(groupId);
        permissions.forEach(this::put);
    }

    private void refreshSingle(PermissionKey key) {
        remove(key.groupId(), key.permission());
        GroupPermission permission = loadByKey(key);
        if (permission != null)
            put(permission);
    }

    private List<GroupPermission> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupPermission());
    }

    private List<GroupPermission> loadByUserId(int groupId) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupPermission(),
                stmt -> stmt.setInt(1, groupId));
    }

    private GroupPermission loadByKey(PermissionKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ? AND permission = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.groupPermission(),
                stmt -> {
                    stmt.setInt(1, key.groupId());
                    stmt.setString(2, key.permission());
                });
    }

    public GroupPermission get(int groupId, String permission) {
        return cacheByKey.get(new PermissionKey(groupId, permission));
    }

    public List<GroupPermission> getPermissions(int groupId) {
        return cacheByGroupId.get(groupId);
    }

    public void put(GroupPermission groupPermission) {
        PermissionKey key = new PermissionKey(groupPermission.groupId(), groupPermission.permission());
        cacheByKey.put(key, groupPermission);
        CacheUtil.put(cacheByGroupId, groupPermission.groupId(), groupPermission,
                v -> v.groupId() == groupPermission.groupId() && v.permission().equals(groupPermission.permission()));
        CacheUtil.put(listCache, ALL_KEY, groupPermission,
                v -> v.groupId() == groupPermission.groupId() && v.permission().equals(groupPermission.permission()));
    }

    public void remove(int groupId, String permission) {
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

    public List<GroupPermission> getCachedPermissions() {
        List<GroupPermission> permissions = listCache.get(ALL_KEY);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    protected record PermissionKey(int groupId, String permission) {
    }
}
