package de.murmelmeister.murmelapi.maintenance.whitelist;

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

public class MaintenanceWhitelistCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWhitelistCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_MAINTENANCE = "SELECT * FROM %s WHERE maintenance_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_USER = "SELECT * FROM %s WHERE user_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<MaintenanceWhitelist>> cacheById;
    private final LoadingCache<@NotNull Integer, List<MaintenanceWhitelist>> cacheByMaintenanceId;
    private final LoadingCache<@NotNull Integer, List<MaintenanceWhitelist>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<MaintenanceWhitelist>> listCache;

    public MaintenanceWhitelistCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByMaintenanceId = CacheUtil.buildCacheRefresh(this::loadByMaintenanceId, cacheCapacity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.MAINTENANCES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_MAINTENANCE.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof MaintenanceWhitelist whitelist)
                remove(whitelist);
            else if (key instanceof String json) {
                try {
                    final MaintenanceWhitelist whitelist = gson.fromJson(json, MaintenanceWhitelist.class);

                    if (whitelist == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(whitelist);
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

    private @NotNull List<MaintenanceWhitelist> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.maintenanceWhitelist());
    }

    private @NotNull List<MaintenanceWhitelist> loadByMaintenanceId(int maintenanceId) {
        String sql = SELECT_BY_MAINTENANCE.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.maintenanceWhitelist(), stmt -> stmt.setInt(1, maintenanceId));
    }

    private @NotNull List<MaintenanceWhitelist> loadByUserId(int userId) {
        String sql = SELECT_BY_USER.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.maintenanceWhitelist(), stmt -> stmt.setInt(1, userId));
    }

    private @NotNull Optional<MaintenanceWhitelist> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        MaintenanceWhitelist maintenanceWhitelist = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.maintenanceWhitelist(),
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(maintenanceWhitelist);
    }

    public @Nullable MaintenanceWhitelist getById(int id) {
        Optional<MaintenanceWhitelist> optWhitelist = cacheById.get(id);
        return optWhitelist != null && optWhitelist.isPresent() ? optWhitelist.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<MaintenanceWhitelist> getByMaintenanceId(int maintenanceId) {
        List<MaintenanceWhitelist> list = cacheByMaintenanceId.get(maintenanceId);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return List.copyOf(list);
    }

    public @NotNull @Unmodifiable List<MaintenanceWhitelist> getByUserId(int userId) {
        List<MaintenanceWhitelist> list = cacheByUserId.get(userId);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return List.copyOf(list);
    }

    public @NotNull @Unmodifiable List<MaintenanceWhitelist> getAll() {
        List<MaintenanceWhitelist> list = listCache.get(ALL_KEY);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return List.copyOf(list);
    }

    public void remove(@NotNull MaintenanceWhitelist whitelist) {
        cacheById.invalidate(whitelist.id());
        cacheByMaintenanceId.invalidate(whitelist.maintenanceId());
        cacheByUserId.invalidate(whitelist.userId());
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == whitelist.id());
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByMaintenanceId.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }
}
