package de.murmelmeister.murmelapi.clan.parent;

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

public class ClanParentCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*clanId=([^,]+), groupId=([^,]+), parentId=(\\d+).*");
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull ParentKey, ClanParent> cacheByKey;
    private final LoadingCache<@NotNull GroupKey, List<ClanParent>> cacheByGroup;
    private final LoadingCache<@NotNull String, List<ClanParent>> listCache;
    private final Long fetchLimit;

    public ClanParentCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByGroup = CacheUtil.buildCacheRefresh(this::loadByGroup, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.CLAN_PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_CLAN_PARENT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof ParentKey parentKey)
                    refreshSingle(parentKey);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    UUID clanId = UUID.fromString(matcher.group(1));
                    UUID groupId = UUID.fromString(matcher.group(2));
                    int parentId = Integer.parseInt(matcher.group(3));
                    refreshSingle(new ParentKey(clanId, groupId, parentId));
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
        List<ClanParent> parents = loadAllFromDatabase();
        if (parents.isEmpty())
            return;
        parents.forEach(this::put);
    }

    private void refreshSingle(ParentKey key) {
        remove(key.clanId(), key.groupId(), key.parentId());
        ClanParent parent = loadByKey(key);
        if (parent != null) put(parent);
    }

    private List<ClanParent> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanParent());
    }

    private List<ClanParent> loadByGroup(GroupKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE clan_id = ? AND group_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanParent(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
        });
    }

    private ClanParent loadByKey(ParentKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE clan_id = ? AND group_id = ? AND parent_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clanParent(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
            stmt.setInt(3, key.parentId());
        });
    }

    public ClanParent get(UUID clanId, UUID groupId, int parentId) {
        return cacheByKey.get(new ParentKey(clanId, groupId, parentId));
    }

    public List<ClanParent> getByGroup(UUID clanId, UUID groupId) {
        return cacheByGroup.get(new GroupKey(clanId, groupId));
    }

    public List<ClanParent> getAll() {
        List<ClanParent> parents = listCache.get(ALL_KEY);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public void put(ClanParent clanParent) {
        cacheByKey.put(new ParentKey(clanParent.clanId(), clanParent.groupId(), clanParent.parentId()), clanParent);
        CacheUtil.put(cacheByGroup, new GroupKey(clanParent.clanId(), clanParent.groupId()), clanParent,
                v -> v.clanId().equals(clanParent.clanId()) && v.groupId().equals(clanParent.groupId()));
        CacheUtil.put(listCache, ALL_KEY, clanParent, v -> v.clanId().equals(clanParent.clanId()) && v.groupId().equals(clanParent.groupId()));
    }

    public void remove(UUID clanId, UUID groupId, int parentId) {
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

    protected record ParentKey(UUID clanId, UUID groupId, int parentId) {
    }

    protected record GroupKey(UUID clanId, UUID groupId) {
    }
}
