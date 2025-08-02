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
    private final LoadingCache<ColorKey, GroupColor> cacheByKey;
    private final LoadingCache<String, List<GroupColor>> listCache;
    private final Long fetchLimit;

    public GroupColorCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
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
            if (key instanceof ColorKey(int groupId, int typeId))
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

    private GroupColor loadByKey(ColorKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ? AND type_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.groupColor(), key.groupId(), key.typeId());
    }

    public GroupColor get(int groupId, int typeId) {
        return cacheByKey.get(new ColorKey(groupId, typeId));
    }

    public void put(GroupColor groupColor) {
        ColorKey key = new ColorKey(groupColor.groupId(), groupColor.typeId());
        cacheByKey.put(key, groupColor);
        CacheUtil.put(listCache, ALL_KEY, groupColor,
                v -> v.groupId() == groupColor.groupId() && v.typeId() == groupColor.typeId());
    }

    public void remove(int groupId, int typeId) {
        ColorKey key = new ColorKey(groupId, typeId);
        cacheByKey.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == groupId && v.typeId() == typeId);
    }

    public void remove(int groupId) {
        cacheByKey.asMap().keySet().stream().filter(key -> key.groupId() == groupId)
                .forEach(cacheByKey::invalidate);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == groupId);
    }

    public void clear() {
        cacheByKey.invalidateAll();
        listCache.invalidateAll();
    }

    public List<GroupColor> getCachedColors() {
        return listCache.get(ALL_KEY);
    }

    protected record ColorKey(int groupId, int typeId) {
    }
}
