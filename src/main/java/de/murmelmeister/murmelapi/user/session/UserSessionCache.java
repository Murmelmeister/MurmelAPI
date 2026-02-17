package de.murmelmeister.murmelapi.user.session;

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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserSessionCache implements MurmelCache {
    private final Logger logger = LoggerFactory.getLogger(UserSessionCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull UUID, Optional<UserSession>> cacheById;
    private final LoadingCache<@NotNull Integer, Optional<UserSession>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<UserSession>> listCache;

    public UserSessionCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
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
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            clear();
        else if (RefreshType.SINGLE_USER_SESSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof UserSession session)
                remove(session);
             else if (key instanceof String json) {
                 final Gson gson = new Gson();
                 try {
                     final UserSession session = gson.fromJson(json, UserSession.class);
                     remove(session);
                 } catch (JsonSyntaxException e) {
                     logger.error("Failed to parse UserSession from JSON: {}", json, e);
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
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userSession());
    }

    private @NotNull Optional<UserSession> loadByUserId(int userId) {
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        UserSession session = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userSession(),
                stmt -> stmt.setInt(1, userId));
        return Optional.ofNullable(session);
    }

    private @NotNull Optional<UserSession> loadById(UUID sessionId) {
        String sql = SELECT_BY_ID.formatted(tableName);
        UserSession session = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userSession(),
                stmt -> stmt.setString(1, sessionId.toString()));
        return Optional.ofNullable(session);
    }

    public @Nullable UserSession getById(@Nullable UUID sessionId) {
        if (sessionId == null) return null;
        Optional<UserSession> optSession = cacheById.get(sessionId);
        return optSession != null && optSession.isPresent() ? optSession.orElse(null) : null;
    }

    public @Nullable UserSession getByUserId(int userId) {
        Optional<UserSession> optSession = cacheByUserId.get(userId);
        return optSession != null && optSession.isPresent() ? optSession.orElse(null) : null;
    }

    public void remove(@NotNull UserSession session) {
        cacheById.invalidate(session.id());
        cacheByUserId.invalidate(session.userId());
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(session.id()));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<UserSession> getCachedSessions() {
        List<UserSession> sessions = listCache.get(ALL_KEY);
        if (sessions == null || sessions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(sessions);
    }
}
