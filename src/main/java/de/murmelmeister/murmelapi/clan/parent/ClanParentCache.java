package de.murmelmeister.murmelapi.clan.parent;

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

public class ClanParentCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ? AND parent_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*clanId=([^,]+), groupId=([^,]+), parentId=(\\d+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ParentKey, Optional<ClanParent>> cacheByKey;
    private final LoadingCache<@NotNull GroupKey, List<ClanParent>> cacheByGroup;
    private final LoadingCache<@NotNull String, List<ClanParent>> listCache;

    public ClanParentCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByGroup = CacheUtil.buildCacheRefresh(this::loadByGroup, cacheCapcity, refreshInterval);
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
            if (!(key instanceof String)) {
                if (key instanceof ParentKey(UUID clanId, UUID groupId, int parentId))
                    remove(clanId, groupId, parentId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    UUID clanId = UUID.fromString(matcher.group(1));
                    UUID groupId = UUID.fromString(matcher.group(2));
                    int parentId = Integer.parseInt(matcher.group(3));
                    remove(clanId, groupId, parentId);
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

    private @NotNull List<ClanParent> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanParent());
    }

    private @NotNull List<ClanParent> loadByGroup(GroupKey key) {
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
            stmt.setInt(3, key.parentId());
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
        return cacheByGroup.get(new GroupKey(clanId, groupId));
    }

    public @NotNull List<ClanParent> getAll() {
        List<ClanParent> parents = listCache.get(ALL_KEY);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public void put(@Nullable ClanParent clanParent) {
        if (clanParent == null) return;
        cacheByKey.put(new ParentKey(clanParent.clanId(), clanParent.groupId(), clanParent.parentId()), Optional.of(clanParent));
        CacheUtil.put(cacheByGroup, new GroupKey(clanParent.clanId(), clanParent.groupId()), clanParent,
                v -> v.clanId().equals(clanParent.clanId()) && v.groupId().equals(clanParent.groupId()));
        CacheUtil.put(listCache, ALL_KEY, clanParent, v -> v.clanId().equals(clanParent.clanId()) && v.groupId().equals(clanParent.groupId()));
    }

    public void remove(@NotNull UUID clanId, @NotNull UUID groupId, int parentId) {
        ParentKey key = new ParentKey(clanId, groupId, parentId);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByGroup, new GroupKey(clanId, groupId), v -> v.clanId().equals(clanId) && v.groupId().equals(groupId));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.clanId().equals(clanId) && v.groupId().equals(groupId));
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByGroup.invalidateAll();
        listCache.invalidateAll();
    }

    protected record ParentKey(@NotNull UUID clanId, @NotNull UUID groupId, int parentId) {
    }

    protected record GroupKey(@NotNull UUID clanId, @NotNull UUID groupId) {
    }
}
