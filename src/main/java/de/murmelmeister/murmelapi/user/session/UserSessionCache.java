package de.murmelmeister.murmelapi.user.session;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class UserSessionCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSessionCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull UUID, Optional<UserSession>> cacheById;
    private final LoadingCache<@NotNull Integer, Optional<UserSession>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<UserSession>> listCache;

    public UserSessionCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.USER_SESSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_SESSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof SessionKey session)
                remove(session);
            else if (key instanceof String json) {
                try {
                    final SessionKey session = gson.fromJson(json, SessionKey.class);

                    if (session == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(session);
                } catch (JsonSyntaxException e) {
                    LOGGER.error("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<UserSession> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, UserSessionRowMapper::resultSet);
    }

    private @NotNull Optional<UserSession> loadByUserId(int userId) {
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        UserSession session = CacheUtil.loadSingle(database, sql, fetchLimit, UserSessionRowMapper::resultSet,
                stmt -> stmt.setInt(1, userId));
        return Optional.ofNullable(session);
    }

    private @NotNull Optional<UserSession> loadById(UUID sessionId) {
        String sql = SELECT_BY_ID.formatted(tableName);
        UserSession session = CacheUtil.loadSingle(database, sql, fetchLimit, UserSessionRowMapper::resultSet,
                stmt -> stmt.setString(1, sessionId.toString()));
        return Optional.ofNullable(session);
    }

    public @NotNull Optional<UserSession> getById(@NotNull UUID sessionId) {
        return cacheById.get(sessionId);
    }

    public @NotNull Optional<UserSession> getByUserId(int userId) {
        return cacheByUserId.get(userId);
    }

    public @NotNull @Unmodifiable List<UserSession> getAll() {
        List<UserSession> sessions = listCache.get(ALL_KEY);
        if (sessions == null || sessions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(sessions);
    }

    public void remove(@NotNull SessionKey session) {
        cacheById.invalidate(session.id());
        cacheByUserId.invalidate(session.userId());
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }

    record SessionKey(UUID id, int userId) {
    }
}
