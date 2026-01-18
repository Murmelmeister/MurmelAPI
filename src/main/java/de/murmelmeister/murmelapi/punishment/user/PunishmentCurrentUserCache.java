package de.murmelmeister.murmelapi.punishment.user;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentCurrentUserCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE user_id = ? AND type_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*userId=(\\d+), typeId=(\\d+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull UserTypeKey, Optional<PunishmentCurrentUser>> cache;
    private final LoadingCache<@NotNull String, List<PunishmentCurrentUser>> listCache;

    public PunishmentCurrentUserCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapcity, refreshInterval);
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
            if (!(key instanceof String)) {
                if (key instanceof UserTypeKey(int userId, int typeId))
                    remove(userId, typeId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    int typeId = Integer.parseInt(matcher.group(2));
                    remove(userId, typeId);
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

    public void put(@Nullable PunishmentCurrentUser punish) {
        if (punish == null) return;
        UserTypeKey key = new UserTypeKey(punish.userId(), punish.typeId());
        cache.put(key, Optional.of(punish));
        CacheUtil.put(listCache, ALL_KEY, punish,
                v -> v.userId() == punish.userId() && v.typeId() == punish.typeId());
    }

    public void remove(int userId, int typeId) {
        UserTypeKey key = new UserTypeKey(userId, typeId);
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY,
                v -> v.userId() == userId && v.typeId() == typeId);
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

    protected record UserTypeKey(int userId, int typeId) {
    }
}
