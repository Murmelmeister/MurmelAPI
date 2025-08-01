package de.murmelmeister.murmelapi.user.permission;

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

public class UserPermissionCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<PermissionKey, UserPermission> cacheByKey;
    private final LoadingCache<Integer, List<UserPermission>> cacheByUserId;
    private final LoadingCache<String, List<UserPermission>> listCache;
    private final Long fetchLimit;

    public UserPermissionCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.USER_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (key instanceof PermissionKey(int userId, String permission))
                remove(userId, permission);
            else if (key instanceof Integer userId)
                remove(userId);
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<UserPermission> permissions = loadAllFromDatabase();
        permissions.forEach(this::put);
    }

    private List<UserPermission> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPermission());
    }

    private List<UserPermission> loadByUserId(int userId) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPermission(), userId);
    }

    private UserPermission loadByKey(PermissionKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ? AND permission = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userPermission(), key.userId(), key.permission());
    }

    public UserPermission get(int userId, String permission) {
        return cacheByKey.get(new PermissionKey(userId, permission));
    }

    public List<UserPermission> getPermissions(int userId) {
        return cacheByUserId.get(userId);
    }

    public void put(UserPermission permission) {
        PermissionKey key = new PermissionKey(permission.userId(), permission.permission());
        cacheByKey.put(key, permission);
        CacheUtil.put(cacheByUserId, permission.userId(), permission,
                v -> v.userId() == permission.userId() && v.permission().equals(permission.permission()));
        CacheUtil.put(listCache, ALL_KEY, permission, v -> v.userId() == permission.userId() && v.permission().equals(permission.permission()));
    }

    public void remove(int userId, String permission) {
        PermissionKey key = new PermissionKey(userId, permission);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByUserId, userId, v -> v.userId() == userId && v.permission().equals(permission));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId && v.permission().equals(permission));
    }

    public void remove(int userId) {
        cacheByKey.asMap().keySet().stream().filter(key -> key.userId() == userId)
                .forEach(cacheByKey::invalidate);
        cacheByUserId.invalidate(userId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId);
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }

    public List<UserPermission> getCachedPermissions() {
        return listCache.get(ALL_KEY);
    }

    protected record PermissionKey(int userId, String permission) {
    }
}
