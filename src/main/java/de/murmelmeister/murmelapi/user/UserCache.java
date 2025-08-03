package de.murmelmeister.murmelapi.user;

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
import java.util.UUID;

public class UserCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<Integer, User> cacheById;
    private final LoadingCache<UUID, User> cacheByUUID;
    private final LoadingCache<String, User> cacheByName;
    private final LoadingCache<String, List<User>> listCache;
    private final Long fetchLimit;

    public UserCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByUUID = CacheUtil.buildCacheRefresh(this::loadByUUID, cacheCapacity, refreshInterval);
        this.cacheByName = CacheUtil.buildCacheRefresh(this::loadByName, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.USERS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
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
        List<User> users = loadAllFromDatabase();
        users.forEach(this::put);
    }

    private void refreshSingle(int id) {
        remove(id);
        User user = loadById(id);
        if (user != null)
            put(user);
    }

    private List<User> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.user());
    }

    private User loadByName(String name) {
        String sql = "SELECT * FROM " + tableName + " WHERE username = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(), name);
    }

    private User loadByUUID(UUID uuid) {
        String sql = "SELECT * FROM " + tableName + " WHERE mojang_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(), uuid.toString());
    }

    private User loadById(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(), id);
    }

    public User getById(int id) {
        return cacheById.get(id);
    }

    public User getByUUID(UUID uuid) {
        return cacheByUUID.get(uuid);
    }

    public User getByName(String name) {
        return cacheByName.get(name);
    }

    public void put(User user) {
        cacheById.put(user.id(), user);
        cacheByUUID.put(user.mojangId(), user);
        cacheByName.put(user.username(), user);
        CacheUtil.put(listCache, ALL_KEY, user, v -> v.id() == user.id());
    }

    public void remove(int id) {
        User user = cacheById.getIfPresent(id);
        if (user != null) {
            cacheById.invalidate(id);
            cacheByUUID.invalidate(user.mojangId());
            cacheByName.invalidate(user.username());
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == id);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUUID.invalidateAll();
        cacheByName.invalidateAll();
        listCache.invalidateAll();
    }

    public List<User> getCachedUsers() {
        return listCache.get(ALL_KEY);
    }
}
