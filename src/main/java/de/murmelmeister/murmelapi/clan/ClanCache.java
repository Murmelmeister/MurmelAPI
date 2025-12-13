package de.murmelmeister.murmelapi.clan;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ClanCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull UUID, Clan> cacheById;
    private final LoadingCache<@NotNull String, Clan> cacheByName;
    private final LoadingCache<@NotNull Integer, Clan> cacheByOwner;
    private final LoadingCache<@NotNull String, List<Clan>> listCache;
    private final Long fetchLimit;

    public ClanCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByName = CacheUtil.buildCacheRefresh(this::loadByName, cacheCapacity, refreshInterval);
        this.cacheByOwner = CacheUtil.buildCacheRefresh(this::loadByOwner, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.CLANS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_CLAN.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof String uuid)
                refreshSingle(UUID.fromString(uuid));
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<Clan> clans = loadAllFromDatabase();
        if (clans.isEmpty()) return;
        clans.forEach(this::put);
    }

    private void refreshSingle(UUID uuid) {
        remove(uuid);
        Clan clan = loadById(uuid);
        if (clan != null) put(clan);
    }

    private List<Clan> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clan());
    }

    private Clan loadById(UUID uuid) {
        if (uuid == null) return null;
        String sql = "SELECT * FROM " + tableName + " WHERE clan_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clan(), stmt -> stmt.setObject(1, uuid));
    }

    private Clan loadByName(String name) {
        String sql = "SELECT * FROM " + tableName + " WHERE clan_name = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clan(), stmt -> stmt.setString(1, name));
    }

    private Clan loadByOwner(int ownerId) {
        String sql = "SELECT * FROM " + tableName + " WHERE owner_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clan(), stmt -> stmt.setInt(1, ownerId));
    }

    public Clan getById(UUID uuid) {
        return cacheById.get(uuid);
    }

    public Clan getByName(String name) {
        return cacheByName.get(name);
    }

    public Clan getByOwner(int ownerId) {
        return cacheByOwner.get(ownerId);
    }

    public void put(Clan clan) {
        cacheById.put(clan.id(), clan);
        cacheByName.put(clan.name(), clan);
        cacheByOwner.put(clan.ownerId(), clan);
        CacheUtil.put(listCache, ALL_KEY, clan, v -> v.id().equals(clan.id()));
    }

    public void remove(UUID clanId) {
        Clan clan = cacheById.getIfPresent(clanId);
        if (clan != null) {
            cacheById.invalidate(clanId);
            cacheByName.invalidate(clan.name());
            cacheByOwner.invalidate(clan.ownerId());
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(clanId));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByName.invalidateAll();
        cacheByOwner.invalidateAll();
        listCache.invalidateAll();
    }

    public List<Clan> getAll() {
        List<Clan> clans = listCache.get(ALL_KEY);
        if (clans == null || clans.isEmpty())
            return Collections.emptyList();
        return clans;
    }
}
