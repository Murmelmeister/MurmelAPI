package de.murmelmeister.murmelapi.clan;

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

final class ClanCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClanCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_NAME = "SELECT * FROM %s WHERE name = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_OWNER = "SELECT * FROM %s WHERE owner_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull UUID, Optional<Clan>> cacheById;
    private final LoadingCache<@NotNull String, Optional<Clan>> cacheByName;
    private final LoadingCache<@NotNull Integer, Optional<Clan>> cacheByOwner;
    private final LoadingCache<@NotNull String, List<Clan>> listCache;

    public ClanCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByName = CacheUtil.buildCacheRefresh(this::loadByName, cacheCapacity, refreshInterval);
        this.cacheByOwner = CacheUtil.buildCacheRefresh(this::loadByOwner, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.CLANS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_CLAN.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof ClanKey clan)
                remove(clan);
            else if (key instanceof String json) {
                try {
                    final ClanKey clan = gson.fromJson(json, ClanKey.class);

                    if (clan == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(clan);
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

    private @NotNull List<Clan> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ClanRowMapper::resultSet);
    }

    private @NotNull Optional<Clan> loadById(UUID uuid) {
        if (uuid == null) return Optional.empty();
        String sql = SELECT_BY_ID.formatted(tableName);
        Clan clan = CacheUtil.loadSingle(database, sql, fetchLimit, ClanRowMapper::resultSet,
                stmt -> stmt.setObject(1, uuid));

        return Optional.ofNullable(clan);
    }

    private @NotNull Optional<Clan> loadByName(String name) {
        String sql = SELECT_BY_NAME.formatted(tableName);
        Clan clan = CacheUtil.loadSingle(database, sql, fetchLimit, ClanRowMapper::resultSet,
                stmt -> stmt.setString(1, name));

        return Optional.ofNullable(clan);
    }

    private @NotNull Optional<Clan> loadByOwner(int ownerId) {
        String sql = SELECT_BY_OWNER.formatted(tableName);
        Clan clan = CacheUtil.loadSingle(database, sql, fetchLimit, ClanRowMapper::resultSet,
                stmt -> stmt.setInt(1, ownerId));

        return Optional.ofNullable(clan);
    }

    public @NotNull Optional<Clan> getById(@NotNull UUID uuid) {
        return cacheById.get(uuid);
    }

    public @NotNull Optional<Clan> getByName(@NotNull String name) {
        return cacheByName.get(name);
    }

    public @NotNull Optional<Clan> getByOwner(int ownerId) {
        return cacheByOwner.get(ownerId);
    }

    public @NotNull @Unmodifiable List<Clan> getAll() {
        List<Clan> clans = listCache.get(ALL_KEY);
        if (clans == null || clans.isEmpty())
            return Collections.emptyList();
        return clans;
    }

    public void remove(@NotNull ClanKey clan) {
        cacheById.invalidate(clan.id());
        cacheByName.invalidate(clan.name());
        cacheByOwner.invalidate(clan.ownerId());
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByName.invalidateAll();
        cacheByOwner.invalidateAll();
        listCache.invalidateAll();
    }

    record ClanKey(@NotNull UUID id, @NotNull String name, int ownerId) {
    }
}
