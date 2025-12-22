package de.murmelmeister.murmelapi.clan.permission;

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

public class ClanPermissionCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*clanId=([^,]+), groupId=([^,]+), permission=([^,\\]]+).*");
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull PermissionKey, ClanPermission> cacheByKey;
    private final LoadingCache<@NotNull GroupKey, List<ClanPermission>> cacheByGroup;
    private final LoadingCache<@NotNull String, List<ClanPermission>> listCache;
    private final Long fetchLimit;

    public ClanPermissionCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByGroup = CacheUtil.buildCacheRefresh(this::loadByGroup, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapcity, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.CLAN_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_CLAN_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof PermissionKey permissionKey)
                    refreshSingle(permissionKey);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    UUID clanId = UUID.fromString(matcher.group(1));
                    UUID groupId = UUID.fromString(matcher.group(2));
                    String permission = matcher.group(3);
                    refreshSingle(new PermissionKey(clanId, groupId, permission));
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
        List<ClanPermission> permissions = loadAllFromDatabase();
        if (permissions.isEmpty())
            return;
        permissions.forEach(this::put);
    }

    private void refreshSingle(PermissionKey key) {
        remove(key.clanId(), key.groupId(), key.permission());
        ClanPermission permission = loadByKey(key);
        if (permission != null) put(permission);
    }

    private List<ClanPermission> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanPermission());
    }

    private List<ClanPermission> loadByGroup(GroupKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE clan_id = ? AND group_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanPermission(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
        });
    }

    private ClanPermission loadByKey(PermissionKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE clan_id = ? AND group_id = ? AND permission = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clanPermission(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
            stmt.setString(3, key.permission());
        });
    }

    public ClanPermission get(UUID clanId, UUID groupId, String permission) {
        return cacheByKey.get(new PermissionKey(clanId, groupId, permission));
    }

    public List<ClanPermission> getByPermissions(UUID clanId, UUID groupId) {
        return cacheByGroup.get(new GroupKey(clanId, groupId));
    }

    public List<ClanPermission> getAll() {
        List<ClanPermission> permissions = listCache.get(ALL_KEY);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    public void put(ClanPermission clanPermission) {
        cacheByKey.put(new PermissionKey(clanPermission.clanId(), clanPermission.groupId(), clanPermission.permission()), clanPermission);
        CacheUtil.put(cacheByGroup, new GroupKey(clanPermission.clanId(), clanPermission.groupId()), clanPermission,
                v -> v.groupId().equals(clanPermission.groupId()));
        CacheUtil.put(listCache, ALL_KEY, clanPermission, v -> v.groupId().equals(clanPermission.groupId()));
    }

    public void remove(UUID clanId, UUID groupId, String permission) {
        PermissionKey key = new PermissionKey(clanId, groupId, permission);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByGroup, new GroupKey(clanId, groupId), v -> v.groupId().equals(groupId));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId().equals(groupId));
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByGroup.invalidateAll();
        listCache.invalidateAll();
    }

    protected record PermissionKey(UUID clanId, UUID groupId, String permission) {
    }

    protected record GroupKey(UUID clanId, UUID groupId) {
    }
}
