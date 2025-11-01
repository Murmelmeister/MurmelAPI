package de.murmelmeister.murmelapi.punishment.user;

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
        String cacheName = event.type();
        if (RefreshType.PUNISHMENT_USERS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_PUNISHMENT_USER.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof UserTypeKey userTypeKey)
                    refreshSingle(userTypeKey);
            } else {
                Matcher matcher = Pattern.compile(".*userId=(\\d+), typeId=(\\d+).*").matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    int typeId = Integer.parseInt(matcher.group(2));
                    refreshSingle(new UserTypeKey(userId, typeId));
                } else {
                    throw new IllegalArgumentException("Invalid key format: " + key);
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
        List<PunishmentCurrentUser> punishments = loadAllFromDatabase();
        punishments.forEach(this::put);
    }

    private void refreshSingle(UserTypeKey key) {
        remove(key.userId(), key.typeId());
        PunishmentCurrentUser punishment = loadFromDatabase(key);
        if (punishment != null)
            put(punishment);
    }

    private List<PunishmentCurrentUser> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentUser());
    }

    private PunishmentCurrentUser loadFromDatabase(UserTypeKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ? AND type_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentUser(),
                stmt -> {
                    stmt.setInt(1, key.userId());
                    stmt.setInt(2, key.typeId());
                });
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
        List<PunishmentCurrentUser> users = listCache.get(ALL_KEY);
        if (users == null || users.isEmpty())
            return Collections.emptyList();
        return List.copyOf(users);
    }

    protected record UserTypeKey(int userId, int typeId) {
    }
}
