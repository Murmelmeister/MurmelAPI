package de.murmelmeister.murmelapi.punishment.user;

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

public class PunishmentCurrentUserCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentCurrentUserCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE user_id = ? AND type_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull UserTypeKey, Optional<PunishmentCurrentUser>> cache;
    private final LoadingCache<@NotNull String, List<PunishmentCurrentUser>> listCache;

    public PunishmentCurrentUserCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.PUNISHMENT_USERS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PUNISHMENT_USER.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof UserTypeKey userTypeKey)
                remove(userTypeKey);
            else if (key instanceof String json) {
                final Gson gson = new Gson();
                try {
                    final UserTypeKey userTypeKey = gson.fromJson(json, UserTypeKey.class);
                    remove(userTypeKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single punishment user refresh: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<PunishmentCurrentUser> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentUser());
    }

    private @NotNull Optional<PunishmentCurrentUser> loadFromDatabase(UserTypeKey key) {
        String sql = SELECT_BY_ID.formatted(tableName);
        PunishmentCurrentUser punishmentCurrentUser = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentUser(),
                stmt -> {
                    stmt.setInt(1, key.userId());
                    stmt.setInt(2, key.typeId());
                });

        return Optional.ofNullable(punishmentCurrentUser);
    }

    public @Nullable PunishmentCurrentUser get(int userId, int typeId) {
        Optional<PunishmentCurrentUser> optUser = cache.get(new UserTypeKey(userId, typeId));
        return optUser != null && optUser.isPresent() ? optUser.orElse(null) : null;
    }

    public void remove(@NotNull UserTypeKey key) {
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == key.userId() && v.typeId() == key.typeId());
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<PunishmentCurrentUser> getCachedPunishUsers() {
        List<PunishmentCurrentUser> users = listCache.get(ALL_KEY);
        if (users == null || users.isEmpty())
            return Collections.emptyList();
        return List.copyOf(users);
    }

    public record UserTypeKey(int userId, int typeId) {
    }
}
