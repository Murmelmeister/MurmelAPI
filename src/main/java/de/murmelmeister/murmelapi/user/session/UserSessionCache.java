package de.murmelmeister.murmelapi.user.session;

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

public class UserSessionCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<UUID, UserSession> cacheById;
    private final LoadingCache<Integer, UserSession> cacheByUserId;
    private final LoadingCache<String, List<UserSession>> listCache;
    private final Long fetchLimit;

    public UserSessionCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.USER_SESSIONS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER_SESSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
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
        List<UserSession> sessions = loadAllFromDatabase();
        sessions.forEach(this::put);
    }

    private void refreshSingle(UUID sessionId) {
        remove(sessionId);
        UserSession session = loadById(sessionId);
        if (session != null)
            put(session);
    }

    private List<UserSession> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userSession());
    }

    private UserSession loadByUserId(int userId) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userSession(), userId);
    }

    private UserSession loadById(UUID sessionId) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userSession(), sessionId.toString());
    }

    public UserSession getById(UUID sessionId) {
        return cacheById.get(sessionId);
    }

    public UserSession getByUserId(int userId) {
        return cacheByUserId.get(userId);
    }

    public void put(UserSession session) {
        cacheById.put(session.id(), session);
        cacheByUserId.put(session.userId(), session);
        CacheUtil.put(listCache, ALL_KEY, session, v -> v.id().equals(session.id()));
    }

    public void remove(UUID sessionId) {
        UserSession session = cacheById.get(sessionId);
        if (session != null) {
            cacheById.invalidate(sessionId);
            cacheByUserId.invalidate(session.userId());
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(sessionId));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }

    public List<UserSession> getCachedSessions() {
        return listCache.get(ALL_KEY);
    }
}
