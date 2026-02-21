package de.murmelmeister.murmelapi.inventory;

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

public class InventoryTypeCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryTypeCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<InventoryType>> cacheById;
    private final LoadingCache<@NotNull String, List<InventoryType>> listCache;

    public InventoryTypeCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.INVENTORY_TYPES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_INVENTORY_TYPE.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof InventoryType type)
                remove(type);
            else if (key instanceof String json) {
                try {
                    final InventoryType type = gson.fromJson(json, InventoryType.class);

                    if (type == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(type);
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

    private @NotNull List<InventoryType> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.inventoryType());
    }

    private @NotNull Optional<InventoryType> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        InventoryType inventoryType = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.inventoryType(),
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(inventoryType);
    }

    public @Nullable InventoryType getById(int id) {
        Optional<InventoryType> optType = cacheById.get(id);
        return optType != null && optType.isPresent() ? optType.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<InventoryType> getAll() {
        List<InventoryType> types = listCache.get(ALL_KEY);
        if (types == null || types.isEmpty())
            return Collections.emptyList();
        return types;
    }

    public void remove(@NotNull InventoryType type) {
        cacheById.invalidate(type.id());
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == type.id());
    }

    public void clear() {
        cacheById.invalidateAll();
        listCache.invalidateAll();
    }
}
