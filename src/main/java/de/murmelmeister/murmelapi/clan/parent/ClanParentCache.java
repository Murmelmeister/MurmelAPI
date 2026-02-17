package de.murmelmeister.murmelapi.clan.parent;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClanParentCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClanParentCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ? AND parent_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ParentKey, Optional<ClanParent>> cacheByKey;
    private final LoadingCache<@NotNull ParentKey, List<ClanParent>> cacheByGroup;
    private final LoadingCache<@NotNull String, List<ClanParent>> listCache;

    public ClanParentCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByGroup = CacheUtil.buildCacheRefresh(this::loadByGroup, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.CLAN_PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_CLAN_PARENT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof ParentKey parentKey)
                remove(parentKey);
            else if (key instanceof String json) {
                final Gson gson = new Gson();
                try {
                    final ParentKey parentKey = gson.fromJson(json, ParentKey.class);
                    remove(parentKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.error("Failed to parse JSON for single clan parent refresh", e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<ClanParent> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanParent());
    }

    private @NotNull List<ClanParent> loadByGroup(ParentKey key) {
        String sql = SELECT_BY_GROUP.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanParent(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
        });
    }

    private @NotNull Optional<ClanParent> loadByKey(ParentKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        ClanParent clanParent = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clanParent(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
            if (key.parentId() != null) stmt.setInt(3, key.parentId());
            else stmt.setNull(3, Types.INTEGER);
        });

        return Optional.ofNullable(clanParent);
    }

    public @Nullable ClanParent get(@Nullable UUID clanId, @Nullable UUID groupId, int parentId) {
        if (clanId == null || groupId == null) return null;
        Optional<ClanParent> optParent = cacheByKey.get(new ParentKey(clanId, groupId, parentId));
        return optParent != null && optParent.isPresent() ? optParent.orElse(null) : null;
    }

    public @Nullable List<ClanParent> getByGroup(@Nullable UUID clanId, @Nullable UUID groupId) {
        if (clanId == null || groupId == null) return null;
        return cacheByGroup.get(new ParentKey(clanId, groupId, null));
    }

    public @NotNull List<ClanParent> getAll() {
        List<ClanParent> parents = listCache.get(ALL_KEY);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public void remove(@NotNull ParentKey key) {
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByGroup, key, v -> v.clanId().equals(key.clanId()) && v.groupId().equals(key.groupId()));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.clanId().equals(key.clanId()) && v.groupId().equals(key.groupId()));
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByGroup.invalidateAll();
        listCache.invalidateAll();
    }

    public record ParentKey(@NotNull UUID clanId, @NotNull UUID groupId, @Nullable Integer parentId) {
    }
}
