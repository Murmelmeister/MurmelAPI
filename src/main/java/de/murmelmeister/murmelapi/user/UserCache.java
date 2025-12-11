package de.murmelmeister.murmelapi.user;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public class UserCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull Integer, User> cacheById;
    private final LoadingCache<@NotNull UUID, User> cacheByUUID;
    private final LoadingCache<@NotNull String, User> cacheByName;
    private final LoadingCache<@NotNull String, List<User>> listCache;
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
        String cacheName = event.type();
        if (RefreshType.USERS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            Integer id = null;
            if (key instanceof Number number)
                id = number.intValue();
            else if (key instanceof String stringKey) {
                try {
                    id = Integer.parseInt(stringKey);
                } catch (NumberFormatException ignored) {
                    return;
                }
            }
            if (id != null)
                refreshSingle(id);
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
        if (users.isEmpty())
            return;
        users.forEach(user -> {
            cacheById.put(user.id(), user);
            if (!isBlocked(user)) {
                if (user.mojangId() != null) cacheByUUID.put(user.mojangId(), user);
                cacheByName.put(user.username(), user);
            }
        });
        listCache.put(ALL_KEY, List.copyOf(
                users.stream().filter(user -> !isBlocked(user)).toList()
        ));
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
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(),
                stmt -> stmt.setString(1, name));
    }

    private User loadByUUID(UUID uuid) {
        if (uuid == null) return null;
        String sql = "SELECT * FROM " + tableName + " WHERE mojang_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(),
                stmt -> stmt.setString(1, uuid.toString()));
    }

    private User loadById(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(),
                stmt -> stmt.setInt(1, id));
    }

    public User getById(int id) {
        return cacheById.get(id);
    }

    public User getByUUID(UUID uuid) {
        if (uuid == null) return null;
        User user = cacheByUUID.get(uuid);
        return isBlocked(user) ? null : user;
    }

    public User getByName(String name) {
        if (name == null) return null;
        User user = cacheByName.get(name);
        return isBlocked(user) ? null : user;
    }

    public void put(User user) {
        if (user == null) return;
        cacheById.put(user.id(), user);
        if (!isBlocked(user)) {
            if (user.mojangId() != null) cacheByUUID.put(user.mojangId(), user);
            cacheByName.put(user.username(), user);
            CacheUtil.put(listCache, ALL_KEY, user, v -> v.id() == user.id());
        }
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
        List<User> users = listCache.get(ALL_KEY);
        if (users == null || users.isEmpty())
            return Collections.emptyList();
        return List.copyOf(users);
    }

    private boolean isBlocked(User user) {
        return user == null || user.id() == CONSOLE_USER_ID || user.systemUser();
    }
}
