package de.murmelmeister.murmelapi.user.excuse;

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

public class UserExcuseCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserExcuseCache.class);

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

    private final LoadingCache<@NotNull Integer, Optional<UserExcuse>> cacheById;
    private final LoadingCache<@NotNull Integer, List<UserExcuse>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<UserExcuse>> listCache;

    public UserExcuseCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
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

        if (RefreshType.USER_EXCUSES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_EXCUSE.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof UserExcuse excuse)
                remove(excuse);
            else if (key instanceof String json) {
                try {
                    final UserExcuse excuse = gson.fromJson(json, UserExcuse.class);

                    if (excuse == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(excuse);
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

    private @NotNull List<UserExcuse> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userExcuse());
    }

    private @NotNull List<UserExcuse> loadByUserId(int userId) {
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userExcuse(),
                stmt -> stmt.setInt(1, userId));
    }

    private @NotNull Optional<UserExcuse> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        UserExcuse excuse = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userExcuse(),
                stmt -> stmt.setInt(1, id));
        return Optional.ofNullable(excuse);
    }

    public @Nullable UserExcuse getById(int id) {
        Optional<UserExcuse> optExcuse = cacheById.get(id);
        return optExcuse != null && optExcuse.isPresent() ? optExcuse.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<UserExcuse> getByUserId(int userId) {
        List<UserExcuse> list = cacheByUserId.get(userId);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return List.copyOf(list);
    }

    public @NotNull @Unmodifiable List<UserExcuse> getAll() {
        List<UserExcuse> list = listCache.get(ALL_KEY);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return List.copyOf(list);
    }

    public void remove(@NotNull UserExcuse excuse) {
        cacheById.invalidate(excuse.id());
        cacheByUserId.invalidate(excuse.userId());
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }
}
