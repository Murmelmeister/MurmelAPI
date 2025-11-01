package de.murmelmeister.murmelapi.group.parent;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String cacheName = event.type();
        if (RefreshType.GROUP_PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_GROUP_PARENT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof ParentKey parentKey)
                    refreshSingle(parentKey);
                else if (key instanceof Integer groupId)
                    refreshSingle(groupId);
            } else {
                Matcher matcher = Pattern.compile(".*groupId=(\\d+), parentId=(\\d+).*").matcher((String) key);
                if (matcher.matches()) {
                    int groupId = Integer.parseInt(matcher.group(1));
                    int parentId = Integer.parseInt(matcher.group(2));
                    refreshSingle(new ParentKey(groupId, parentId));
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
        List<GroupParent> parents = loadAllFromDatabase();
        parents.forEach(this::put);
    }

    private void refreshSingle(int groupId) {
        remove(groupId);
        List<GroupParent> parents = loadByGroupId(groupId);
        parents.forEach(this::put);
    }

    private void refreshSingle(ParentKey key) {
        remove(key.groupId(), key.parentId());
        GroupParent groupParent = loadByKey(key);
        if (groupParent != null)
            put(groupParent);
    }

    public List<GroupParent> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupParent());
    }

    public List<GroupParent> loadByGroupId(int groupId) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupParent(),
                stmt -> stmt.setInt(1, groupId));
    }

    private GroupParent loadByKey(ParentKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ? AND parent_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.groupParent(),
                stmt -> {
                    stmt.setInt(1, key.groupId());
                    stmt.setInt(2, key.parentId());
                });
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
        List<GroupParent> parents = listCache.get(ALL_KEY);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    protected record ParentKey(int groupId, int parentId) {
    }
}
