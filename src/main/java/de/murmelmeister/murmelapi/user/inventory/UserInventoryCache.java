package de.murmelmeister.murmelapi.user.inventory;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserInventoryCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*userId=(\\d+), inventoryId=(\\d+).*");
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull InventoryKey, UserInventory> cacheByKey;
    private final LoadingCache<@NotNull String, List<UserInventory>> listCache;
    private final Long fetchLimit;

    public UserInventoryCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapcity, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.USER_INVENTORIES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER_INVENTORY.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof InventoryKey inventoryKey)
                    refreshSingle(inventoryKey);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    int inventoryId = Integer.parseInt(matcher.group(2));
                    refreshSingle(new InventoryKey(userId, inventoryId));
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
        List<UserInventory> inventories = loadAllFromDatabase();
        if (inventories.isEmpty())
            return;
        inventories.forEach(this::put);
    }

    private void refreshSingle(InventoryKey key) {
        remove(key.userId(), key.inventoryId());
        UserInventory inventory = loadById(key);
        if (inventory != null) put(inventory);
    }

    private List<UserInventory> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userInventory());
    }

    private UserInventory loadById(InventoryKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ? AND inventory_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userInventory(), stmt -> {
            stmt.setInt(1, key.userId());
            stmt.setInt(2, key.inventoryId());
        });
    }

    public UserInventory get(int userId, int inventoryId) {
        return cacheByKey.get(new InventoryKey(userId, inventoryId));
    }

    public List<UserInventory> getAll() {
        List<UserInventory> inventories = listCache.get(ALL_KEY);
        if (inventories == null || inventories.isEmpty())
            return Collections.emptyList();
        return inventories;
    }

    public void put(UserInventory inventory) {
        cacheByKey.put(new InventoryKey(inventory.userId(), inventory.inventoryId()), inventory);
        CacheUtil.put(listCache, ALL_KEY, inventory, v -> v.userId() == inventory.userId() && v.inventoryId() == inventory.inventoryId());
    }

    public void remove(int userId, int inventoryId) {
        cacheByKey.invalidate(new InventoryKey(userId, inventoryId));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId && v.inventoryId() == inventoryId);
    }

    public void clear() {
        cacheByKey.invalidateAll();
        listCache.invalidateAll();
    }

    protected record InventoryKey(int userId, int inventoryId) {
    }
}
