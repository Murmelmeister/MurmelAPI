package de.murmelmeister.murmelapi.user.login;

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

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserLoginCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserLoginCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_IP_ADDRESS = "SELECT * FROM %s WHERE ip_address = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull UUID, Optional<UserLogin>> cacheById;
    private final LoadingCache<@NotNull Integer, List<UserLogin>> cacheByUserId;
    private final LoadingCache<@NotNull InetAddress, List<UserLogin>> cacheByIpAddress;
    private final LoadingCache<@NotNull String, List<UserLogin>> listCache;

    public UserLoginCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.cacheByIpAddress = CacheUtil.buildCacheRefresh(this::loadByIpAddress, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.USER_LOGINS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_LOGIN.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof UserLogin login)
                remove(login);
            else if (key instanceof String json) {
                try {
                    final UserLogin login = gson.fromJson(json, UserLogin.class);

                    if (login == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(login);
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

    private @NotNull List<UserLogin> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin());
    }

    private @NotNull List<UserLogin> loadByUserId(int userId) {
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin(),
                stmt -> stmt.setInt(1, userId));
    }

    private @NotNull List<UserLogin> loadByIpAddress(InetAddress inetAddress) {
        String sql = SELECT_BY_IP_ADDRESS.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin(),
                stmt -> stmt.setString(1, inetAddress.getHostAddress()));
    }

    private @NotNull Optional<UserLogin> loadById(UUID id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        UserLogin login = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userLogin(),
                stmt -> stmt.setString(1, id.toString()));
        return Optional.ofNullable(login);
    }

    public @Nullable UserLogin getById(@Nullable UUID id) {
        if (id == null) return null;
        Optional<UserLogin> optLogin = cacheById.get(id);
        return optLogin != null && optLogin.isPresent() ? optLogin.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<UserLogin> getByUserId(int userId) {
        List<UserLogin> list = cacheByUserId.get(userId);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return List.copyOf(list);
    }

    public @NotNull @Unmodifiable List<UserLogin> getByIpAddress(@Nullable InetAddress inetAddress) {
        if (inetAddress == null) return Collections.emptyList();
        List<UserLogin> list = cacheByIpAddress.get(inetAddress);
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        return List.copyOf(list);
    }

    public @NotNull @Unmodifiable List<UserLogin> getAll() {
        List<UserLogin> logins = listCache.get(ALL_KEY);
        if (logins == null || logins.isEmpty())
            return Collections.emptyList();
        return List.copyOf(logins);
    }

    public void remove(@NotNull UserLogin login) {
        cacheById.invalidate(login.id());
        cacheByUserId.invalidate(login.userId());
        cacheByIpAddress.invalidate(login.inetAddress());
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUserId.invalidateAll();
        cacheByIpAddress.invalidateAll();
        listCache.invalidateAll();
    }
}
