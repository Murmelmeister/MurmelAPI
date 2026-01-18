package de.murmelmeister.murmelapi.color;

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

public class PrefixColorCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull String, Optional<PrefixColor>> cacheById;
    private final LoadingCache<@NotNull String, List<PrefixColor>> listCache;

    public PrefixColorCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapcity, refreshInterval);
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
            if (key instanceof String) remove((String) key);
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

    public @NotNull List<PrefixColor> getAll() {
        List<PrefixColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return colors;
    }

    public void put(@Nullable PrefixColor color) {
        if (color == null) return;
        cacheById.put(color.id(), Optional.of(color));
        CacheUtil.put(listCache, ALL_KEY, color, v -> v.id().equals(color.id()));
    }

    public void remove(@NotNull String id) {
        cacheById.invalidate(id);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(id));
    }

    public void clear() {
        cacheById.invalidateAll();
        listCache.invalidateAll();
    }
}
