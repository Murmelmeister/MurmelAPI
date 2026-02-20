package de.murmelmeister.murmelapi.user.inventory;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UserInventoryCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserInventoryCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE user_id = ? AND inventory_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull InventoryKey, Optional<UserInventory>> cacheByKey;
    private final LoadingCache<@NotNull String, List<UserInventory>> listCache;

    public UserInventoryCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
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
            if (key instanceof InventoryKey inventoryKey)
                remove(inventoryKey);
            else if (key instanceof String json) {
                try {
                    final InventoryKey inventoryKey = gson.fromJson(json, InventoryKey.class);

                    if (inventoryKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(inventoryKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
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

    public @NotNull @Unmodifiable List<UserInventory> getAll() {
        List<UserInventory> inventories = listCache.get(ALL_KEY);
        if (inventories == null || inventories.isEmpty())
            return Collections.emptyList();
        return inventories;
    }

    public void remove(@NotNull InventoryKey key) {
        cacheByKey.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == key.userId() && v.inventoryId() == key.inventoryId());
    }

    public void clear() {
        cacheByKey.invalidateAll();
        listCache.invalidateAll();
    }

    public record InventoryKey(int userId, int inventoryId) {
    }
}
