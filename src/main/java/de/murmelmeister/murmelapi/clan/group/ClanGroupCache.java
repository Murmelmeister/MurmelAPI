package de.murmelmeister.murmelapi.clan.group;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClanGroupCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*clanId=([^,]+), groupId=([^,]+).*");
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull GroupKey, ClanGroup> cacheByKey;
    private final LoadingCache<@NotNull UUID, List<ClanGroup>> cacheByClanId;
    private final LoadingCache<@NotNull String, List<ClanGroup>> listCache;
    private final Long fetchLimit;

    public ClanGroupCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByClanId = CacheUtil.buildCacheRefresh(this::loadByClanId, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapcity, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.CLAN_GROUPS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_CLAN_GROUP.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof GroupKey groupKey)
                    refreshSingle(groupKey);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    UUID clanId = UUID.fromString(matcher.group(1));
                    UUID groupId = UUID.fromString(matcher.group(2));
                    refreshSingle(new GroupKey(clanId, groupId));
                } else {
                    throw new IllegalArgumentException("Invalid key format: " + key);
                }
            }
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<ClanGroup> clans = loadAllFromDatabase();
        if (clans.isEmpty())
            return;
        clans.forEach(this::put);
    }

    private void refreshSingle(GroupKey key) {
        remove(key.clanId(), key.groupId());
        ClanGroup group = loadByKey(key);
        if (group != null) put(group);
    }

    private List<ClanGroup> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanGroup());
    }

    private List<ClanGroup> loadByClanId(UUID clanId) {
        String sql = "SELECT * FROM " + tableName + " WHERE clan_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanGroup(), stmt -> stmt.setString(1, clanId.toString()));
    }

    private ClanGroup loadByKey(GroupKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE clan_id = ? AND group_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clanGroup(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
        });
    }

    public ClanGroup getByKey(UUID clanId, UUID groupId) {
        return cacheByKey.get(new GroupKey(clanId, groupId));
    }

    public List<ClanGroup> getByClanId(UUID clanId) {
        return cacheByClanId.get(clanId);
    }

    public List<ClanGroup> getAll() {
        List<ClanGroup> clans = listCache.get(ALL_KEY);
        if (clans == null || clans.isEmpty())
            return Collections.emptyList();
        return clans;
    }

    public void put(ClanGroup group) {
        GroupKey key = new GroupKey(group.clanId(), group.groupId());
        cacheByKey.put(key, group);
        CacheUtil.put(cacheByClanId, group.clanId(), group, v -> v.clanId().equals(group.clanId()));
        CacheUtil.put(listCache, ALL_KEY, group, v -> v.clanId().equals(group.clanId()) && v.groupId().equals(group.groupId()));
    }

    public void remove(UUID clanId, UUID groupId) {
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

    protected record GroupKey(UUID clanId, UUID groupId) {
    }
}
