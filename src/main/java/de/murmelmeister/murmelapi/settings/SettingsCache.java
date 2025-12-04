package de.murmelmeister.murmelapi.settings;

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

public class SettingsCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull String, Settings> cache;
    private final LoadingCache<@NotNull String, List<Settings>> listCache;
    private final Long fetchLimit;

    public SettingsCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadSingleFromDatabase, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.SETTINGS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_SETTING.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof String tagId)
                refreshSingle(tagId);
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    public void refreshAll() {
        clear();
        List<Settings> settings = loadAllFromDatabase();
        if (settings.isEmpty())
            return;
        settings.forEach(this::put);
    }

    public void refreshSingle(String tagId) {
        remove(tagId);
        Settings settings = loadSingleFromDatabase(tagId);
        if (settings != null)
            put(settings);
    }

    private List<Settings> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.settings());
    }

    private Settings loadSingleFromDatabase(String tagId) {
        String sql = "SELECT * FROM " + tableName + " WHERE tag_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.settings(),
                stmt -> stmt.setString(1, tagId));
    }

    public Settings get(String tagId) {
        return cache.get(tagId);
    }

    public List<Settings> getCachedSettings() {
        List<Settings> settings = listCache.get(ALL_KEY);
        if (settings == null || settings.isEmpty())
            return Collections.emptyList();
        return List.copyOf(settings);
    }

    public void put(Settings settings) {
        cache.put(settings.tagId(), settings);
        CacheUtil.put(listCache, ALL_KEY, settings, v -> v.tagId().equals(settings.tagId()));
    }

    public void remove(String tagId) {
        cache.invalidate(tagId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.tagId().equals(tagId));
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }
}
