package de.murmelmeister.murmelapi.settings;

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

public class SettingsCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE tag_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull String, Optional<Settings>> cache;
    private final LoadingCache<@NotNull String, List<Settings>> listCache;

    public SettingsCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadSingleFromDatabase, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.SETTINGS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_SETTING.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof String tagId)
                remove(tagId);
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<Settings> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.settings());
    }

    private @NotNull Optional<Settings> loadSingleFromDatabase(String tagId) {
        String sql = SELECT_BY_ID.formatted(tableName);
        Settings settings = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.settings(),
                stmt -> stmt.setString(1, tagId));

        return Optional.ofNullable(settings);
    }

    public @Nullable Settings get(@Nullable String tagId) {
        if (tagId == null) return null;
        Optional<Settings> optSettings = cache.get(tagId);
        return optSettings != null && optSettings.isPresent() ? optSettings.orElse(null) : null;
    }

    public @NotNull List<Settings> getCachedSettings() {
        List<Settings> settings = listCache.get(ALL_KEY);
        if (settings == null || settings.isEmpty())
            return Collections.emptyList();
        return List.copyOf(settings);
    }

    public void put(@Nullable Settings settings) {
        if (settings == null) return;
        cache.put(settings.tagId(), Optional.of(settings));
        CacheUtil.put(listCache, ALL_KEY, settings, v -> v.tagId().equals(settings.tagId()));
    }

    public void remove(@NotNull String tagId) {
        cache.invalidate(tagId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.tagId().equals(tagId));
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }
}
