package de.murmelmeister.murmelapi.clan.permission;

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

public class ClanPermissionCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClanPermissionCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE clan_id = ? AND group_id = ? AND permission = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PermissionKey, Optional<ClanPermission>> cacheByKey;
    private final LoadingCache<@NotNull PermissionKey, List<ClanPermission>> cacheByGroup;
    private final LoadingCache<@NotNull String, List<ClanPermission>> listCache;

    public ClanPermissionCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByGroup = CacheUtil.buildCacheRefresh(this::loadByGroup, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
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
            if (key instanceof PermissionKey permissionKey)
                remove(permissionKey);
            else if (key instanceof String json) {
                final Gson gson = new Gson();
                try {
                    final PermissionKey permissionKey = gson.fromJson(json, PermissionKey.class);
                    remove(permissionKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single clan permission refresh: {}", json, e);
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

    private @NotNull List<ClanPermission> loadByGroup(PermissionKey key) {
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
            if (key.permission() != null) stmt.setString(3, key.permission());
            else stmt.setNull(3, Types.VARCHAR);
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
        return cacheByGroup.get(new PermissionKey(clanId, groupId, null));
    }

    public @NotNull List<ClanPermission> getAll() {
        List<ClanPermission> permissions = listCache.get(ALL_KEY);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    public void remove(@NotNull PermissionKey key) {
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByGroup, key, v -> v.groupId().equals(key.groupId()));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId().equals(key.groupId()));
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByGroup.invalidateAll();
        listCache.invalidateAll();
    }

    public record PermissionKey(@NotNull UUID clanId, @NotNull UUID groupId, @Nullable String permission) {
    }
}
