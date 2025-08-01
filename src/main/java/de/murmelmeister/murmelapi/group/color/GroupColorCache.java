package de.murmelmeister.murmelapi.group.color;

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

public class GroupColorCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<GroupColorKey, GroupColor> cacheByKey;
    private final LoadingCache<Integer, List<GroupColor>> cacheByGroupId;
    private final LoadingCache<String, List<GroupColor>> listCache;
    private final Long fetchLimit;

    public GroupColorCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByGroupId = CacheUtil.buildCacheRefresh(this::loadByGroupId, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.GROUP_COLORS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_GROUP_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (key instanceof GroupColorKey(int groupId, int typeId))
                remove(groupId, typeId);
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
        List<GroupColor> colors = loadAllFromDatabase();
        colors.forEach(this::put);
    }

    private List<GroupColor> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupColor());
    }

    private List<GroupColor> loadByGroupId(int groupId) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupColor(), groupId);
    }

    private GroupColor loadByKey(GroupColorKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ? AND type_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.groupColor(), key.groupId(), key.typeId());
    }

    public GroupColor get(int groupId, int typeId) {
        return cacheByKey.get(new GroupColorKey(groupId, typeId));
    }

    public List<GroupColor> getByGroupId(int groupId) {
        return cacheByGroupId.get(groupId);
    }

    public void put(GroupColor groupColor) {
        cacheByKey.put(new GroupColorKey(groupColor.groupId(), groupColor.typeId()), groupColor);
        CacheUtil.put(cacheByGroupId, groupColor.groupId(), groupColor,
                v -> v.groupId() == groupColor.groupId() && v.typeId() == groupColor.typeId());
        CacheUtil.put(listCache, ALL_KEY, groupColor,
                v -> v.groupId() == groupColor.groupId() && v.typeId() == groupColor.typeId());
    }

    public void remove(int groupId, int typeId) {
        cacheByKey.invalidate(new GroupColorKey(groupId, typeId));
        CacheUtil.remove(cacheByGroupId, groupId, v -> v.groupId() == groupId && v.typeId() == typeId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == groupId && v.typeId() == typeId);
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

    public List<GroupColor> getCachedColors() {
        return listCache.get(ALL_KEY);
    }

    protected record GroupColorKey(int groupId, int typeId) {
    }
}
