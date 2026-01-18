package de.murmelmeister.murmelapi.user.inventory;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserInventoryCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE user_id = ? AND inventory_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*userId=(\\d+), inventoryId=(\\d+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull InventoryKey, Optional<UserInventory>> cacheByKey;
    private final LoadingCache<@NotNull String, List<UserInventory>> listCache;

    public UserInventoryCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapcity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.USER_INVENTORIES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_INVENTORY.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof InventoryKey(int userId, int inventoryId))
                    remove(userId, inventoryId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    int inventoryId = Integer.parseInt(matcher.group(2));
                    remove(userId, inventoryId);
                } else {
                    throw new IllegalArgumentException("Invalid key format: " + key);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<UserInventory> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userInventory());
    }

    private @NotNull Optional<UserInventory> loadById(InventoryKey key) {
        String sql = SELECT_BY_ID.formatted(tableName);
        UserInventory userInventory = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userInventory(), stmt -> {
            stmt.setInt(1, key.userId());
            stmt.setInt(2, key.inventoryId());
        });

        return Optional.ofNullable(userInventory);
    }

    public @Nullable UserInventory get(int userId, int inventoryId) {
        Optional<UserInventory> optInv = cacheByKey.get(new InventoryKey(userId, inventoryId));
        return optInv != null && optInv.isPresent() ? optInv.orElse(null) : null;
    }

    public @NotNull List<UserInventory> getAll() {
        List<UserInventory> inventories = listCache.get(ALL_KEY);
        if (inventories == null || inventories.isEmpty())
            return Collections.emptyList();
        return inventories;
    }

    public void put(@Nullable UserInventory inventory) {
        if (inventory == null) return;
        cacheByKey.put(new InventoryKey(inventory.userId(), inventory.inventoryId()), Optional.of(inventory));
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
