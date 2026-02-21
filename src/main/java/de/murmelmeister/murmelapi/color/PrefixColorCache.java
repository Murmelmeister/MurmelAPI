package de.murmelmeister.murmelapi.color;

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

public class PrefixColorCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrefixColorCache.class);

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

    private final LoadingCache<@NotNull String, Optional<PrefixColor>> cacheById;
    private final LoadingCache<@NotNull String, List<PrefixColor>> listCache;

    public PrefixColorCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
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

        if (RefreshType.PREFIX_COLORS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PREFIX_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof PrefixColor color)
                remove(color);
            else if (key instanceof String json) {
                try {
                    final PrefixColor color = gson.fromJson(json, PrefixColor.class);

                    if (color == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(color);
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

    private @NotNull List<PrefixColor> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.prefixColor());
    }

    private @NotNull Optional<PrefixColor> loadById(String id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        PrefixColor prefixColor = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.prefixColor(),
                stmt -> stmt.setString(1, id));

        return Optional.ofNullable(prefixColor);
    }

    public @Nullable PrefixColor getById(@Nullable String id) {
        if (id == null) return null;
        Optional<PrefixColor> optColor = cacheById.get(id);
        return optColor != null && optColor.isPresent() ? optColor.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<PrefixColor> getAll() {
        List<PrefixColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return colors;
    }

    public void remove(@NotNull PrefixColor color) {
        cacheById.invalidate(color.id());
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(color.id()));
    }

    public void clear() {
        cacheById.invalidateAll();
        listCache.invalidateAll();
    }
}
