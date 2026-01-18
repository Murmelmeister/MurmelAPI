package de.murmelmeister.murmelapi.clan.group;

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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClanGroupCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE clan_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*clanId=([^,]+), groupId=([^,]+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull GroupKey, Optional<ClanGroup>> cacheByKey;
    private final LoadingCache<@NotNull UUID, List<ClanGroup>> cacheByClanId;
    private final LoadingCache<@NotNull String, List<ClanGroup>> listCache;

    public ClanGroupCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByClanId = CacheUtil.buildCacheRefresh(this::loadByClanId, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapcity, refreshInterval);
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
            if (!(key instanceof String)) {
                if (key instanceof GroupKey(UUID clanId, UUID groupId))
                    remove(clanId, groupId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    UUID clanId = UUID.fromString(matcher.group(1));
                    UUID groupId = UUID.fromString(matcher.group(2));
                    remove(clanId, groupId);
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

    public @Nullable List<ClanGroup> getByClanId(@Nullable UUID clanId) {
        if (clanId == null) return null;
        return cacheByClanId.get(clanId);
    }

    public @NotNull List<ClanGroup> getAll() {
        List<ClanGroup> clans = listCache.get(ALL_KEY);
        if (clans == null || clans.isEmpty())
            return Collections.emptyList();
        return clans;
    }

    public void put(@Nullable ClanGroup group) {
        if (group == null) return;
        GroupKey key = new GroupKey(group.clanId(), group.groupId());
        cacheByKey.put(key, Optional.of(group));
        CacheUtil.put(cacheByClanId, group.clanId(), group, v -> v.clanId().equals(group.clanId()));
        CacheUtil.put(listCache, ALL_KEY, group, v -> v.clanId().equals(group.clanId()) && v.groupId().equals(group.groupId()));
    }

    public void remove(@NotNull UUID clanId, @NotNull UUID groupId) {
        GroupKey key = new GroupKey(clanId, groupId);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByClanId, clanId, v -> v.clanId().equals(clanId));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.clanId().equals(clanId) && v.groupId().equals(groupId));
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByClanId.invalidateAll();
        listCache.invalidateAll();
    }

    protected record GroupKey(@NotNull UUID clanId, @NotNull UUID groupId) {
    }
}
