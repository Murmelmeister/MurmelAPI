package de.murmelmeister.murmelapi.group.permission;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;

public class GroupPermissionCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
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
        String cacheName = event.getType();
        if (RefreshType.GROUP_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_GROUP_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (key instanceof PermissionKey(int groupId, String permission))
                remove(groupId, permission);
            else if (key instanceof Integer groupId)
                remove(groupId);
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
        permissions.forEach(this::put);
    }

    private List<GroupPermission> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupPermission());
    }

    private List<GroupPermission> loadByUserId(int groupId) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupPermission(), groupId);
    }

    private GroupPermission loadByKey(PermissionKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ? AND permission = ?";
        return database.query(sql, null, ResultSetUtil.groupPermission(), key.groupId(), key.permission());
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
        return listCache.get(ALL_KEY);
    }

    protected record PermissionKey(int groupId, String permission) {
    }
}
