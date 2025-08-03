package de.murmelmeister.murmelapi.user.parent;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserParentCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<ParentKey, UserParent> cacheByKey;
    private final LoadingCache<Integer, List<UserParent>> cacheByUserId;
    private final LoadingCache<String, List<UserParent>> listCache;
    private final Long fetchLimit;

    public UserParentCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.USER_PARENTS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER_PARENT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (!(key instanceof String)) {
                if (key instanceof ParentKey parentKey)
                    refreshSingle(parentKey);
                else if (key instanceof Integer userId)
                    refreshSingle(userId);
            } else {
                Matcher matcher = Pattern.compile(".*userId=(\\d+), parentId=(\\d+).*").matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    int parentId = Integer.parseInt(matcher.group(2));
                    refreshSingle(new ParentKey(userId, parentId));
                } else {
                    int userId = Integer.parseInt((String) key);
                    refreshSingle(userId);
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
        List<UserParent> parents = loadAllFromDatabase();
        parents.forEach(this::put);
    }

    private void refreshSingle(int userId) {
        remove(userId);
        List<UserParent> parents = loadByUserId(userId);
        parents.forEach(this::put);
    }

    private void refreshSingle(ParentKey key) {
        remove(key.userId(), key.parentId());
        UserParent userParent = loadByKey(key);
        if (userParent != null)
            put(userParent);
    }

    private List<UserParent> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userParent());
    }

    private List<UserParent> loadByUserId(int userId) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userParent(), userId);
    }

    private UserParent loadByKey(ParentKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ? AND parent_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userParent(), key.userId(), key.parentId());
    }

    public UserParent get(int userId, int parentId) {
        return cacheByKey.get(new ParentKey(userId, parentId));
    }

    public List<UserParent> getParents(int userId) {
        return cacheByUserId.get(userId);
    }

    public void put(UserParent userParent) {
        ParentKey key = new ParentKey(userParent.userId(), userParent.parentId());
        cacheByKey.put(key, userParent);
        CacheUtil.put(cacheByUserId, userParent.userId(), userParent,
                v -> v.userId() == userParent.userId() && v.parentId() == userParent.parentId());
        CacheUtil.put(listCache, ALL_KEY, userParent,
                v -> v.userId() == userParent.userId() && v.parentId() == userParent.parentId());
    }

    public void remove(int userId, int parentId) {
        ParentKey key = new ParentKey(userId, parentId);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByUserId, userId, v -> v.userId() == userId && v.parentId() == parentId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId && v.parentId() == parentId);
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

    public List<UserParent> getCachedParents() {
        return listCache.get(ALL_KEY);
    }

    protected record ParentKey(int userId, int parentId) {
    }
}
