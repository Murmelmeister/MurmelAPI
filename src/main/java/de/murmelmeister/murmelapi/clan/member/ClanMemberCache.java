package de.murmelmeister.murmelapi.clan.member;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClanMemberCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*clanId=([^,]+), userId=(\\d+).*");
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull Member, ClanMember> cache;
    private final LoadingCache<@NotNull String, List<ClanMember>> listCache;
    private final Long fetchLimit;

    public ClanMemberCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.CLAN_MEMBERS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_CLAN_MEMBER.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Member member)
                    refreshSingle(member);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    UUID clanId = UUID.fromString(matcher.group(1));
                    int userId = Integer.parseInt(matcher.group(2));
                    refreshSingle(new Member(clanId, userId));
                }
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
        List<ClanMember> members = loadAllFromDatabase();
        if (members.isEmpty())
            return;
        members.forEach(this::put);
    }

    private void refreshSingle(Member key) {
        remove(key.clanId(), key.userId());
        ClanMember member = loadFromDatabase(key);
        if (member != null)
            put(member);
    }

    private List<ClanMember> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanMember());
    }

    private ClanMember loadFromDatabase(Member member) {
        String sql = "SELECT * FROM " + tableName + " WHERE clan_id = ? AND user_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clanMember(),
                stmt -> {
                    stmt.setObject(1, member.clanId());
                    stmt.setInt(2, member.userId());
                });
    }

    public ClanMember get(UUID clanId, int userId) {
        return cache.get(new Member(clanId, userId));
    }

    public void put(ClanMember member) {
        Member key = new Member(member.clanId(), member.userId());
        cache.put(key, member);
        CacheUtil.put(listCache, ALL_KEY, member, v -> v.clanId().equals(member.clanId()));
    }

    public void remove(UUID clanId, int userId) {
        Member key = new Member(clanId, userId);
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.clanId().equals(clanId));
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public List<ClanMember> getCachedMembers() {
        List<ClanMember> members = listCache.get(ALL_KEY);
        if (members == null || members.isEmpty())
            return Collections.emptyList();
        return List.copyOf(members);
    }

    protected record Member(UUID clanId, int userId) {
    }
}
