package de.murmelmeister.murmelapi.user.login;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class UserLoginCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<UUID, UserLogin> cacheById;
    private final LoadingCache<Integer, List<UserLogin>> cacheByUserId;
    private final LoadingCache<String, List<UserLogin>> cacheByIpAddress;
    private final LoadingCache<String, List<UserLogin>> listCache;
    private final Long fetchLimit;

    public UserLoginCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapcity, refreshInterval);
        this.cacheByIpAddress = CacheUtil.buildCacheRefresh(this::loadByIpAddress, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.USER_LOGINS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER_LOGIN.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof UUID sessionId)
                    refreshSingle(sessionId);
            } else {
                UUID sessionId = UUID.fromString((String) key);
                refreshSingle(sessionId);
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
        List<UserLogin> userLogins = loadAllFromDatabase();
        userLogins.forEach(this::put);
    }

    private void refreshSingle(UUID sessionId) {
        remove(sessionId);
        UserLogin userLogin = loadById(sessionId);
        if (userLogin != null)
            put(userLogin);
    }

    private List<UserLogin> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin());
    }

    private List<UserLogin> loadByUserId(int userId) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin(), userId);
    }

    private List<UserLogin> loadByIpAddress(String ipAddress) {
        String sql = "SELECT * FROM " + tableName + " WHERE ip_address = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin(), ipAddress);
    }

    private UserLogin loadById(UUID id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userLogin(), id.toString());
    }

    public UserLogin getById(UUID id) {
        return cacheById.get(id);
    }

    public List<UserLogin> getByUserId(int userId) {
        return cacheByUserId.get(userId);
    }

    public List<UserLogin> getByIpAddress(String ipAddress) {
        return cacheByIpAddress.get(ipAddress);
    }

    public void put(UserLogin userLogin) {
        cacheById.put(userLogin.id(), userLogin);
        CacheUtil.put(cacheByUserId, userLogin.userId(), userLogin, v -> v.id().equals(userLogin.id()));
        CacheUtil.put(cacheByIpAddress, userLogin.ipAddress(), userLogin, v -> v.id().equals(userLogin.id()));
        CacheUtil.put(listCache, ALL_KEY, userLogin, v -> v.id().equals(userLogin.id()));
    }

    public void remove(UUID id) {
        UserLogin userLogin = cacheById.get(id);
        if (userLogin != null) {
            cacheById.invalidate(id);
            CacheUtil.remove(cacheByUserId, userLogin.userId(), v -> v.id().equals(id));
            CacheUtil.remove(cacheByIpAddress, userLogin.ipAddress(), v -> v.id().equals(id));
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(id));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUserId.invalidateAll();
        cacheByIpAddress.invalidateAll();
        listCache.invalidateAll();
    }

    public List<UserLogin> getCachedLogins() {
        return listCache.get(ALL_KEY);
    }
}
