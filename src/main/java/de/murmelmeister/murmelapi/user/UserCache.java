package de.murmelmeister.murmelapi.user;

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

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public class UserCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_NAME = "SELECT * FROM %s WHERE username = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_UUID = "SELECT * FROM %s WHERE mojang_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<User>> cacheById;
    private final LoadingCache<@NotNull UUID, Optional<User>> cacheByUUID;
    private final LoadingCache<@NotNull String, Optional<User>> cacheByName;
    private final LoadingCache<@NotNull String, List<User>> listCache;

    public UserCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByUUID = CacheUtil.buildCacheRefresh(this::loadByUUID, cacheCapacity, refreshInterval);
        this.cacheByName = CacheUtil.buildCacheRefresh(this::loadByName, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase().stream()
                .filter(user -> !isBlocked(user))
                .toList(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.USERS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER.getName().equalsIgnoreCase(cacheName))
            parseIdFromKey(event.key()).ifPresent(this::remove);
    }

    private Optional<Integer> parseIdFromKey(@Nullable Object key) {
        if (key instanceof Integer i) return Optional.of(i);
        if (key instanceof Number n) return Optional.of(n.intValue());
        if (key instanceof String s && !s.isEmpty() && Character.isDigit(s.charAt(0))) {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
            }
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<User> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.user());
    }

    private Optional<User> loadByName(String name) {
        String sql = SELECT_BY_NAME.formatted(tableName);
        User user = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(),
                stmt -> stmt.setString(1, name));

        return isBlocked(user) ? Optional.empty() : Optional.of(user);
    }

    private Optional<User> loadByUUID(UUID uuid) {
        String sql = SELECT_BY_UUID.formatted(tableName);
        User user = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(),
                stmt -> stmt.setString(1, uuid.toString()));

        return isBlocked(user) ? Optional.empty() : Optional.of(user);
    }

    private Optional<User> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        User user = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.user(),
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(user);
    }

    public @Nullable User getById(int id) {
        Optional<User> optUser = cacheById.get(id);
        return optUser != null && optUser.isPresent() ? optUser.orElse(null) : null;
    }

    public @Nullable User getByUUID(@Nullable UUID uuid) {
        if (uuid == null) return null;
        Optional<User> optUser = cacheByUUID.get(uuid);
        return optUser != null && optUser.isPresent() ? optUser.orElse(null) : null;
    }

    public @Nullable User getByName(@Nullable String name) {
        if (name == null) return null;
        Optional<User> optUser = cacheByName.get(name);
        return optUser != null && optUser.isPresent() ? optUser.orElse(null) : null;
    }

    public void put(@Nullable User user) {
        if (user == null) return;
        cacheById.put(user.id(), Optional.of(user));

        if (!isBlocked(user)) {
            if (user.mojangId() != null) cacheByUUID.put(user.mojangId(), Optional.of(user));
            cacheByName.put(user.username(), Optional.of(user));
            CacheUtil.put(listCache, ALL_KEY, user, v -> v.id() == user.id());
        }
    }

    public void remove(int id) {
        Optional<User> value = cacheById.getIfPresent(id);
        cacheById.invalidate(id);

        if (value != null && value.isPresent()) {
            User user = value.get();
            if (user.mojangId() != null) cacheByUUID.invalidate(user.mojangId());
            cacheByName.invalidate(user.username());
            CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == id);
        } else {
            listCache.invalidate(ALL_KEY);
        }
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUUID.invalidateAll();
        cacheByName.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<User> getCachedUsers() {
        List<User> users = listCache.get(ALL_KEY);
        if (users == null || users.isEmpty())
            return Collections.emptyList();
        return List.copyOf(users);
    }

    private boolean isBlocked(@Nullable User user) {
        return user == null || user.id() == CONSOLE_USER_ID || user.systemUser();
    }
}
