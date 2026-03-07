package de.murmelmeister.murmelapi.clan;

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
import java.util.UUID;

public class ClanCache implements MurmelCache {
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
            if (key instanceof Clan clan)
                remove(clan);
            else if (key instanceof String json) {
                try {
                    final Clan clan = gson.fromJson(json, Clan.class);

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
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clan());
    }

    private @NotNull Optional<Clan> loadById(UUID uuid) {
        if (uuid == null) return Optional.empty();
        String sql = SELECT_BY_ID.formatted(tableName);
        Clan clan = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clan(),
                stmt -> stmt.setObject(1, uuid));

        return Optional.ofNullable(clan);
    }

    private @NotNull Optional<Clan> loadByName(String name) {
        String sql = SELECT_BY_NAME.formatted(tableName);
        Clan clan = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clan(),
                stmt -> stmt.setString(1, name));

        return Optional.ofNullable(clan);
    }

    private @NotNull Optional<Clan> loadByOwner(int ownerId) {
        String sql = SELECT_BY_OWNER.formatted(tableName);
        Clan clan = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clan(),
                stmt -> stmt.setInt(1, ownerId));

        return Optional.ofNullable(clan);
    }

    public @Nullable Clan getById(@Nullable UUID uuid) {
        if (uuid == null) return null;
        Optional<Clan> optClan = cacheById.get(uuid);
        return optClan != null && optClan.isPresent() ? optClan.orElse(null) : null;
    }

    public @Nullable Clan getByName(@Nullable String name) {
        if (name == null) return null;
        Optional<Clan> optClan = cacheByName.get(name);
        return optClan != null && optClan.isPresent() ? optClan.orElse(null) : null;
    }

    public @Nullable Clan getByOwner(int ownerId) {
        Optional<Clan> optClan = cacheByOwner.get(ownerId);
        return optClan != null && optClan.isPresent() ? optClan.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<Clan> getAll() {
        List<Clan> clans = listCache.get(ALL_KEY);
        if (clans == null || clans.isEmpty())
            return Collections.emptyList();
        return clans;
    }

    public void remove(@NotNull Clan clan) {
        cacheById.invalidate(clan.id());
        cacheByName.invalidate(clan.name());
        cacheByOwner.invalidate(clan.ownerId());
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(clan.id()));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByName.invalidateAll();
        cacheByOwner.invalidateAll();
        listCache.invalidateAll();
    }
}
