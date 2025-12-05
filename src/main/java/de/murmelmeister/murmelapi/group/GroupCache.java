package de.murmelmeister.murmelapi.group;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class GroupCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<Integer, Group> cacheById;
    private final LoadingCache<String, Group> cacheByName;
    private final LoadingCache<String, List<Group>> listCache;
    private final Long fetchLimit;

    public GroupCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByName = CacheUtil.buildCacheRefresh(this::loadByName, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.GROUPS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_GROUP.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Integer id)
                    refreshSingle(id);
            } else {
                int id = Integer.parseInt((String) key);
                refreshSingle(id);
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
        List<Group> groups = loadAllFromDatabase();
        if (groups.isEmpty())
            return;
        groups.forEach(group -> {
            cacheById.put(group.id(), group);
            cacheByName.put(group.groupName(), group);
        });
        listCache.put(ALL_KEY, List.copyOf(groups));
    }

    private void refreshSingle(int id) {
        remove(id);
        Group group = loadById(id);
        if (group != null)
            put(group);
    }

    private List<Group> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.group());
    }

    private Group loadByName(String name) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_name = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.group(),
                stmt -> stmt.setString(1, name));
    }

    private Group loadById(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.group(),
                stmt -> stmt.setInt(1, id));
    }

    public Group getById(int id) {
        return cacheById.get(id);
    }

    public Group getByName(String name) {
        return cacheByName.get(name);
    }

    public void put(Group group) {
        cacheById.put(group.id(), group);
        cacheByName.put(group.groupName(), group);
        CacheUtil.put(listCache, ALL_KEY, group, v -> v.id() == group.id());
    }

    public void remove(int id) {
        Group group = cacheById.getIfPresent(id);
        if (group != null) {
            cacheById.invalidate(id);
            cacheByName.invalidate(group.groupName());
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == id);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByName.invalidateAll();
        listCache.invalidateAll();
    }

    public List<Group> getCachedGroups() {
        List<Group> groups = listCache.get(ALL_KEY);
        if (groups == null || groups.isEmpty())
            return Collections.emptyList();
        return List.copyOf(groups);
    }
}
