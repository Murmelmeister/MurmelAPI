package de.murmelmeister.murmelapi.inventory;

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

public class InventoryTypeCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull Integer, InventoryType> cacheById;
    private final LoadingCache<@NotNull String, List<InventoryType>> listCache;
    private final Long fetchLimit;

    public InventoryTypeCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.INVENTORY_TYPES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_INVENTORY_TYPE.getName().equalsIgnoreCase(cacheName)) {
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
        List<InventoryType> types = loadAllFromDatabase();
        if (types.isEmpty())
            return;
        types.forEach(this::put);
    }

    private void refreshSingle(int id) {
        remove(id);
        InventoryType type = loadById(id);
        if (type != null) put(type);
    }

    private List<InventoryType> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.inventoryType());
    }

    private InventoryType loadById(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.inventoryType(), stmt -> stmt.setInt(1, id));
    }

    public InventoryType getById(int id) {
        return cacheById.get(id);
    }

    public List<InventoryType> getAll() {
        List<InventoryType> types = listCache.get(ALL_KEY);
        if (types == null || types.isEmpty())
            return Collections.emptyList();
        return types;
    }

    public void put(InventoryType type) {
        cacheById.put(type.id(), type);
        CacheUtil.put(listCache, ALL_KEY, type, v -> v.id() == type.id());
    }

    public void remove(int id) {
        cacheById.invalidate(id);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == id);
    }

    public void clear() {
        cacheById.invalidateAll();
        listCache.invalidateAll();
    }
}
