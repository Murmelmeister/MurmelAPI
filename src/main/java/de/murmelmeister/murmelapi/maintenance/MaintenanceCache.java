package de.murmelmeister.murmelapi.maintenance;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
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

public class MaintenanceCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceCache.class);

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

    private final LoadingCache<@NotNull Integer, Optional<Maintenance>> cacheById;
    private final LoadingCache<@NotNull String, List<Maintenance>> listCache;

    public MaintenanceCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
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
            if (key instanceof Maintenance maintenance)
                remove(maintenance);
            else if (key instanceof String json) {
                try {
                    final Maintenance maintenance = gson.fromJson(json, Maintenance.class);

                    if (maintenance == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(maintenance);
                } catch (Exception e) {
                    LOGGER.error("Failed to parse JSON for single: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<Maintenance> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.maintenance());
    }

    private @NotNull Optional<Maintenance> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        Maintenance maintenance = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.maintenance(),
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(maintenance);
    }

    public @Nullable Maintenance getById(int id) {
        Optional<Maintenance> optMaintenance = cacheById.get(id);
        return optMaintenance != null && optMaintenance.isPresent() ? optMaintenance.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<Maintenance> getAll() {
        List<Maintenance> list = listCache.get(ALL_KEY);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return List.copyOf(list);
    }

    public void remove(@NotNull Maintenance maintenance) {
        cacheById.invalidate(maintenance.id());
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheById.invalidateAll();
        listCache.invalidateAll();
    }
}
