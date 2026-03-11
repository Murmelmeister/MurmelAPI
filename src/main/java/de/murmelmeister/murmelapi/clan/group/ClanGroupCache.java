package de.murmelmeister.murmelapi.clan.group;

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
import java.util.UUID;

public class ClanGroupCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClanGroupCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE clan_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull GroupKey, Optional<ClanGroup>> cacheByKey;
    private final LoadingCache<@NotNull UUID, List<ClanGroup>> cacheByClanId;
    private final LoadingCache<@NotNull String, List<ClanGroup>> listCache;

    public ClanGroupCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByClanId = CacheUtil.buildCacheRefresh(this::loadByClanId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.CLAN_GROUPS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_CLAN_GROUP.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof GroupKey groupKey)
                remove(groupKey);
            else if (key instanceof String json) {
                try {
                    final GroupKey groupKey = gson.fromJson(json, GroupKey.class);

                    if (groupKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(groupKey);
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

    private @NotNull List<ClanGroup> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanGroup());
    }

    private @NotNull List<ClanGroup> loadByClanId(UUID clanId) {
        String sql = SELECT_BY_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanGroup(), stmt -> stmt.setString(1, clanId.toString()));
    }

    private @NotNull Optional<ClanGroup> loadByKey(GroupKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        ClanGroup clanGroup = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clanGroup(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
        });

        return Optional.ofNullable(clanGroup);
    }

    public @Nullable ClanGroup getByKey(@Nullable UUID clanId, @Nullable UUID groupId) {
        if (clanId == null || groupId == null) return null;
        Optional<ClanGroup> optGroup = cacheByKey.get(new GroupKey(clanId, groupId));
        return optGroup != null && optGroup.isPresent() ? optGroup.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<ClanGroup> getByClanId(@NotNull UUID clanId) {
        List<ClanGroup> groups = cacheByClanId.get(clanId);
        if (groups == null || groups.isEmpty())
            return Collections.emptyList();
        return List.copyOf(groups);
    }

    public @NotNull @Unmodifiable List<ClanGroup> getAll() {
        List<ClanGroup> clans = listCache.get(ALL_KEY);
        if (clans == null || clans.isEmpty())
            return Collections.emptyList();
        return clans;
    }

    public void remove(@NotNull GroupKey key) {
        cacheByKey.invalidate(key);
        cacheByClanId.invalidate(key.clanId());
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByClanId.invalidateAll();
        listCache.invalidateAll();
    }

    public record GroupKey(@NotNull UUID clanId, @NotNull UUID groupId) {
    }
}
