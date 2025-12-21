package de.murmelmeister.murmelapi.color;

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

public class PrefixColorCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull String, PrefixColor> cacheById;
    private final LoadingCache<@NotNull String, List<PrefixColor>> listCache;
    private final Long fetchLimit;

    public PrefixColorCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapcity, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.PREFIX_COLORS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_PREFIX_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof String) refreshSingle((String) key);
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<PrefixColor> colors = loadAllFromDatabase();
        if (colors.isEmpty())
            return;
        colors.forEach(this::put);
    }

    private void refreshSingle(String id) {
        remove(id);
        PrefixColor color = loadById(id);
        if (color != null) put(color);
    }

    private List<PrefixColor> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.prefixColor());
    }

    private PrefixColor loadById(String id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.prefixColor(), stmt -> stmt.setString(1, id));
    }

    public PrefixColor getById(String id) {
        return cacheById.get(id);
    }

    public List<PrefixColor> getAll() {
        List<PrefixColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return colors;
    }

    public void put(PrefixColor color) {
        cacheById.put(color.id(), color);
        CacheUtil.put(listCache, ALL_KEY, color, v -> v.id().equals(color.id()));
    }

    public void remove(String id) {
        cacheById.invalidate(id);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(id));
    }

    public void clear() {
        cacheById.invalidateAll();
        listCache.invalidateAll();
    }
}
