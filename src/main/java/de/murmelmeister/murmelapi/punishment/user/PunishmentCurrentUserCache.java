package de.murmelmeister.murmelapi.punishment.user;

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

public class PunishmentCurrentUserCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<UserTypeKey, PunishmentCurrentUser> cache;
    private final LoadingCache<String, List<PunishmentCurrentUser>> listCache;
    private final Long fetchLimit;

    public PunishmentCurrentUserCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.PUNISHMENT_USERS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_PUNISHMENT_USER.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (key instanceof UserTypeKey(int userId, int typeId))
                remove(userId, typeId);
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<PunishmentCurrentUser> punishments = loadAllFromDatabase();
        punishments.forEach(this::put);
    }

    private List<PunishmentCurrentUser> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentUser());
    }

    private PunishmentCurrentUser loadFromDatabase(UserTypeKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ? AND type_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentUser(), key.userId(), key.typeId());
    }

    public PunishmentCurrentUser get(int userId, int typeId) {
        return cache.get(new UserTypeKey(userId, typeId));
    }

    public void put(PunishmentCurrentUser punish) {
        UserTypeKey key = new UserTypeKey(punish.userId(), punish.typeId());
        cache.put(key, punish);
        CacheUtil.put(listCache, ALL_KEY, punish,
                v -> v.userId() == punish.userId() && v.typeId() == punish.typeId());
    }

    public void remove(int userId, int typeId) {
        UserTypeKey key = new UserTypeKey(userId, typeId);
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY,
                v -> v.userId() == userId && v.typeId() == typeId);
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public List<PunishmentCurrentUser> getCachedPunishUsers() {
        return listCache.get(ALL_KEY);
    }

    protected record UserTypeKey(int userId, int typeId) {
    }
}
