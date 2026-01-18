package de.murmelmeister.murmelapi.clan.permission;

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

public class ClanPermissionCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ? AND permission = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*clanId=([^,]+), groupId=([^,]+), permission=([^,\\]]+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PermissionKey, Optional<ClanPermission>> cacheByKey;
    private final LoadingCache<@NotNull GroupKey, List<ClanPermission>> cacheByGroup;
    private final LoadingCache<@NotNull String, List<ClanPermission>> listCache;

    public ClanPermissionCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByGroup = CacheUtil.buildCacheRefresh(this::loadByGroup, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapcity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.CLAN_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_CLAN_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof PermissionKey(UUID clanId, UUID groupId, String permission))
                    remove(clanId, groupId, permission);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    UUID clanId = UUID.fromString(matcher.group(1));
                    UUID groupId = UUID.fromString(matcher.group(2));
                    String permission = matcher.group(3);
                    remove(clanId, groupId, permission);
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

    private @NotNull List<ClanPermission> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanPermission());
    }

    private @NotNull List<ClanPermission> loadByGroup(GroupKey key) {
        String sql = SELECT_BY_GROUP.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanPermission(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
        });
    }

    private @NotNull Optional<ClanPermission> loadByKey(PermissionKey key) {
        String sql = SELECT_BY_ID.formatted(tableName);
        ClanPermission clanPermission = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clanPermission(), stmt -> {
            stmt.setString(1, key.clanId().toString());
            stmt.setString(2, key.groupId().toString());
            stmt.setString(3, key.permission());
        });

        return Optional.ofNullable(clanPermission);
    }

    public @Nullable ClanPermission get(@Nullable UUID clanId, @Nullable UUID groupId, @Nullable String permission) {
        if (clanId == null || groupId == null || permission == null) return null;
        Optional<ClanPermission> optPermission = cacheByKey.get(new PermissionKey(clanId, groupId, permission));
        return optPermission != null && optPermission.isPresent() ? optPermission.orElse(null) : null;
    }

    public @Nullable List<ClanPermission> getByPermissions(@Nullable UUID clanId, @Nullable UUID groupId) {
        if (clanId == null || groupId == null) return null;
        return cacheByGroup.get(new GroupKey(clanId, groupId));
    }

    public @NotNull List<ClanPermission> getAll() {
        List<ClanPermission> permissions = listCache.get(ALL_KEY);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    public void put(@Nullable ClanPermission clanPermission) {
        if (clanPermission == null) return;
        cacheByKey.put(new PermissionKey(clanPermission.clanId(), clanPermission.groupId(), clanPermission.permission()), Optional.of(clanPermission));
        CacheUtil.put(cacheByGroup, new GroupKey(clanPermission.clanId(), clanPermission.groupId()), clanPermission,
                v -> v.groupId().equals(clanPermission.groupId()));
        CacheUtil.put(listCache, ALL_KEY, clanPermission, v -> v.groupId().equals(clanPermission.groupId()));
    }

    public void remove(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String permission) {
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

    protected record PermissionKey(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String permission) {
    }

    protected record GroupKey(@NotNull UUID clanId, @NotNull UUID groupId) {
    }
}
