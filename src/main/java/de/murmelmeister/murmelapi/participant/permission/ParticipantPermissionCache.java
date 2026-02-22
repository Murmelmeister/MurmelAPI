package de.murmelmeister.murmelapi.participant.permission;

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

public class ParticipantPermissionCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantPermissionCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE participant_id = ? AND permission = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_PARTICIPANT = "SELECT * FROM %s WHERE participant_id = ?";
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PermissionKey, Optional<ParticipantPermission>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<ParticipantPermission>> cacheByParticipant;
    private final LoadingCache<@NotNull String, List<ParticipantPermission>> cacheAll;

    public ParticipantPermissionCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByParticipant = CacheUtil.buildCacheRefresh(this::loadByParticipant, cacheCapacity, refreshInterval);
        this.cacheAll = CacheUtil.buildCacheRefresh(key -> loadAll(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.PARTICIPANT_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PARTICIPANT_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof PermissionKey permissionKey)
                remove(permissionKey);
            else if (key instanceof String json) {
                try {
                    final PermissionKey permissionKey = gson.fromJson(json, PermissionKey.class);

                    if (permissionKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(permissionKey);
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

    private @NotNull Optional<ParticipantPermission> loadByKey(PermissionKey permissionKey) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        ParticipantPermission participantPermission = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.participantPermission(),
                stmt -> {
                    stmt.setInt(1, permissionKey.participantId());
                    stmt.setString(2, permissionKey.permission());
                });
        return Optional.ofNullable(participantPermission);
    }

    private @NotNull List<ParticipantPermission> loadByParticipant(int participantId) {
        String sql = SELECT_BY_PARTICIPANT.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.participantPermission(), stmt -> stmt.setInt(1, participantId));
    }

    private @NotNull List<ParticipantPermission> loadAll() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.participantPermission());
    }

    public @Nullable ParticipantPermission getByKey(int participantId, @NotNull String permission) {
        Optional<ParticipantPermission> optPermission = cacheByKey.get(new PermissionKey(participantId, permission));
        return optPermission != null && optPermission.isPresent() ? optPermission.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<ParticipantPermission> getByParticipant(int participantId) {
        List<ParticipantPermission> permissions = cacheByParticipant.get(participantId);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    public @NotNull @Unmodifiable List<ParticipantPermission> getAll() {
        List<ParticipantPermission> permissions = cacheAll.get(ALL_KEY);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    public void remove(@NotNull PermissionKey permissionKey) {
        cacheByKey.invalidate(permissionKey);
        cacheByParticipant.invalidate(permissionKey.participantId());
        CacheUtil.remove(cacheAll, ALL_KEY, v -> v.participantId() == permissionKey.participantId());
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByParticipant.invalidateAll();
        cacheAll.invalidateAll();
    }

    public record PermissionKey(int participantId, @Nullable String permission) {
    }
}
