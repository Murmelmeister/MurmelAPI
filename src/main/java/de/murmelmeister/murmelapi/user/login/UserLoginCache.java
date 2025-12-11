package de.murmelmeister.murmelapi.user.login;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.time.Duration;
import java.util.*;

public class UserLoginCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull UUID, UserLogin> cacheById;
    private final LoadingCache<@NotNull Integer, List<UserLogin>> cacheByUserId;
    private final LoadingCache<@NotNull InetAddress, List<UserLogin>> cacheByIpAddress;
    private final LoadingCache<@NotNull String, List<UserLogin>> listCache;
    private final Long fetchLimit;

    public UserLoginCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapcity, refreshInterval);
        this.cacheByIpAddress = CacheUtil.buildCacheRefresh(this::loadByIpAddress, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.USER_LOGINS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER_LOGIN.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof UUID sessionId)
                    refreshSingle(sessionId);
            } else {
                UUID sessionId = UUID.fromString((String) key);
                refreshSingle(sessionId);
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
        List<UserLogin> userLogins = loadAllFromDatabase();
        if (userLogins.isEmpty())
            return;

        Map<Integer, List<UserLogin>> byUser = new HashMap<>();
        Map<InetAddress, List<UserLogin>> byIpAddress = new HashMap<>();

        for (UserLogin userLogin : userLogins) {
            cacheById.put(userLogin.id(), userLogin);
            byUser.computeIfAbsent(userLogin.userId(), ignored -> new ArrayList<>()).add(userLogin);
            byIpAddress.computeIfAbsent(userLogin.inetAddress(), ignored -> new ArrayList<>()).add(userLogin);
        }

        byUser.forEach((userId, logins) -> cacheByUserId.put(userId, List.copyOf(logins)));
        byIpAddress.forEach((ip, logins) -> cacheByIpAddress.put(ip, List.copyOf(logins)));
        listCache.put(ALL_KEY, List.copyOf(userLogins));
    }

    private void refreshSingle(UUID sessionId) {
        remove(sessionId);
        UserLogin userLogin = loadById(sessionId);
        if (userLogin != null)
            put(userLogin);
    }

    private List<UserLogin> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin());
    }

    private List<UserLogin> loadByUserId(int userId) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin(),
                stmt -> stmt.setInt(1, userId));
    }

    private List<UserLogin> loadByIpAddress(InetAddress inetAddress) {
        String sql = "SELECT * FROM " + tableName + " WHERE ip_address = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userLogin(),
                stmt -> stmt.setString(1, inetAddress.getHostAddress()));
    }

    private UserLogin loadById(UUID id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userLogin(),
                stmt -> stmt.setString(1, id.toString()));
    }

    public UserLogin getById(UUID id) {
        return cacheById.get(id);
    }

    public List<UserLogin> getByUserId(int userId) {
        return cacheByUserId.get(userId);
    }

    public List<UserLogin> getByIpAddress(InetAddress inetAddress) {
        return cacheByIpAddress.get(inetAddress);
    }

    public void put(UserLogin userLogin) {
        cacheById.put(userLogin.id(), userLogin);
        CacheUtil.put(cacheByUserId, userLogin.userId(), userLogin, v -> v.id().equals(userLogin.id()));
        CacheUtil.put(cacheByIpAddress, userLogin.inetAddress(), userLogin, v -> v.id().equals(userLogin.id()));
        CacheUtil.put(listCache, ALL_KEY, userLogin, v -> v.id().equals(userLogin.id()));
    }

    public void remove(UUID id) {
        UserLogin userLogin = cacheById.getIfPresent(id);
        cacheById.invalidate(id);
        if (userLogin != null) {
            CacheUtil.remove(cacheByUserId, userLogin.userId(), v -> v.id().equals(id));
            CacheUtil.remove(cacheByIpAddress, userLogin.inetAddress(), v -> v.id().equals(id));
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(id));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUserId.invalidateAll();
        cacheByIpAddress.invalidateAll();
        listCache.invalidateAll();
    }

    public List<UserLogin> getCachedLogins() {
        List<UserLogin> logins = listCache.get(ALL_KEY);
        if (logins == null || logins.isEmpty())
            return Collections.emptyList();
        return List.copyOf(logins);
    }
}
