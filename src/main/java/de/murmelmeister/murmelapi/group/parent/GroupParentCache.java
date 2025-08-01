package de.murmelmeister.murmelapi.group.parent;

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

public class GroupParentCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<ParentKey, GroupParent> cacheByKey;
    private final LoadingCache<Integer, List<GroupParent>> cacheByGroupId;
    private final LoadingCache<String, List<GroupParent>> listCache;
    private final Long fetchLimit;

    public GroupParentCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByGroupId = CacheUtil.buildCacheRefresh(this::loadByGroupId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.GROUP_PARENTS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_GROUP_PARENT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (key instanceof ParentKey(int groupId, int parentId))
                remove(groupId, parentId);
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
        List<GroupParent> parents = loadAllFromDatabase();
        parents.forEach(this::put);
    }

    public List<GroupParent> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupParent());
    }

    public List<GroupParent> loadByGroupId(int groupId) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupParent(), groupId);
    }

    private GroupParent loadByKey(ParentKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ? AND parent_id = ?";
        return database.query(sql, null, ResultSetUtil.groupParent(), key.groupId(), key.parentId());
    }

    public GroupParent get(int groupId, int parentId) {
        return cacheByKey.get(new ParentKey(groupId, parentId));
    }

    public List<GroupParent> getParents(int groupId) {
        return cacheByGroupId.get(groupId);
    }

    public void put(GroupParent groupParent) {
        ParentKey key = new ParentKey(groupParent.groupId(), groupParent.parentId());
        cacheByKey.put(key, groupParent);
        CacheUtil.put(cacheByGroupId, groupParent.groupId(), groupParent,
                v -> v.groupId() == groupParent.groupId() && v.parentId() == groupParent.parentId());
        CacheUtil.put(listCache, ALL_KEY, groupParent,
                v -> v.groupId() == groupParent.groupId() && v.parentId() == groupParent.parentId());
    }

    public void remove(int groupId, int parentId) {
        ParentKey key = new ParentKey(groupId, parentId);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByGroupId, groupId, v -> v.groupId() == groupId && v.parentId() == parentId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == groupId && v.parentId() == parentId);
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

    public List<GroupParent> getCachedParents() {
        return listCache.get(ALL_KEY);
    }

    protected record ParentKey(int groupId, int parentId) {
    }
}
