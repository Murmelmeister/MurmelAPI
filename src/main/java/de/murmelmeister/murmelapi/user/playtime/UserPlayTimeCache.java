package de.murmelmeister.murmelapi.user.playtime;

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

public class UserPlayTimeCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<Integer, UserPlayTime> cache;
    private final LoadingCache<String, List<UserPlayTime>> listCache;
    private final Long fetchLimit;

    public UserPlayTimeCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.USER_PLAY_TIMES.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER_PLAY_TIME.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (!(key instanceof String)) {
                if (key instanceof Integer userId)
                    refreshSingle(userId);
            } else {
                int userId = Integer.parseInt((String) key);
                refreshSingle(userId);
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
        List<UserPlayTime> playTimes = loadAllFromDatabase();
        playTimes.forEach(this::put);
    }

    private void refreshSingle(int userId) {
        remove(userId);
        UserPlayTime playTime = loadById(userId);
        if (playTime != null)
            put(playTime);
    }

    private List<UserPlayTime> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPlayTime());
    }

    private UserPlayTime loadById(int userId) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userPlayTime(), userId);
    }

    public UserPlayTime get(int userId) {
        return cache.get(userId);
    }

    public void put(UserPlayTime playTime) {
        cache.put(playTime.getUserId(), playTime);
        CacheUtil.put(listCache, ALL_KEY, playTime, v -> v.getUserId() == playTime.getUserId());
    }

    public void remove(int userId) {
        cache.invalidate(userId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.getUserId() == userId);
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public List<UserPlayTime> getCachedPlayTimes() {
        return listCache.get(ALL_KEY);
    }
}
