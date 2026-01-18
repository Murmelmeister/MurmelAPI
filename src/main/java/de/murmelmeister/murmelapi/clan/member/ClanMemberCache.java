package de.murmelmeister.murmelapi.clan.member;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClanMemberCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_CLAN = "SELECT * FROM %s WHERE clan_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE clan_id = ? AND user_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*clanId=([^,]+), userId=(\\d+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Member, Optional<ClanMember>> cache;
    private final LoadingCache<@NotNull UUID, List<ClanMember>> cacheByClanId;
    private final LoadingCache<@NotNull String, List<ClanMember>> listCache;

    public ClanMemberCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapacity, refreshInterval);
        this.cacheByClanId = CacheUtil.buildCacheRefresh(this::loadByClanId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.CLAN_MEMBERS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_CLAN_MEMBER.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Member(UUID clanId, int userId))
                    remove(clanId, userId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    UUID clanId = UUID.fromString(matcher.group(1));
                    int userId = Integer.parseInt(matcher.group(2));
                    remove(clanId, userId);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<ClanMember> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanMember());
    }

    private @NotNull List<ClanMember> loadByClanId(UUID clanId) {
        String sql = SELECT_BY_CLAN.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.clanMember(),
                stmt -> stmt.setObject(1, clanId));
    }

    private @NotNull Optional<ClanMember> loadFromDatabase(Member member) {
        String sql = SELECT_BY_ID.formatted(tableName);
        ClanMember clanMember = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.clanMember(),
                stmt -> {
                    stmt.setObject(1, member.clanId());
                    stmt.setInt(2, member.userId());
                });

        return Optional.ofNullable(clanMember);
    }

    public @Nullable ClanMember get(@NotNull UUID clanId, int userId) {
        Optional<ClanMember> optMember = cache.get(new Member(clanId, userId));
        return optMember != null && optMember.isPresent() ? optMember.orElse(null) : null;
    }

    public @Nullable List<ClanMember> getByClanId(@Nullable UUID clanId) {
        if (clanId == null) return null;
        return cacheByClanId.get(clanId);
    }

    public void put(@Nullable ClanMember member) {
        if (member == null) return;
        Member key = new Member(member.clanId(), member.userId());
        cache.put(key, Optional.of(member));
        CacheUtil.put(cacheByClanId, member.clanId(), member, v -> v.clanId().equals(member.clanId()));
        CacheUtil.put(listCache, ALL_KEY, member, v -> v.clanId().equals(member.clanId()));
    }

    public void remove(@NotNull UUID clanId, int userId) {
        Member key = new Member(clanId, userId);
        cache.invalidate(key);
        CacheUtil.remove(cacheByClanId, clanId, v -> v.clanId().equals(clanId));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.clanId().equals(clanId));
    }

    public void clear() {
        cache.invalidateAll();
        cacheByClanId.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<ClanMember> getCachedMembers() {
        List<ClanMember> members = listCache.get(ALL_KEY);
        if (members == null || members.isEmpty())
            return Collections.emptyList();
        return List.copyOf(members);
    }

    protected record Member(@NotNull UUID clanId, int userId) {
    }
}
