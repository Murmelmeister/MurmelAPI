package de.murmelmeister.murmelapi.clan.member;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClanMemberCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClanMemberCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_CLAN = "SELECT * FROM %s WHERE clan_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE clan_id = ? AND user_id = ?";

    private static final String ALL_KEY = "ALL";

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
            if (key instanceof Member member)
                remove(member);
            else if (key instanceof String json) {
                final Gson gson = new Gson();
                try {
                    final Member member = gson.fromJson(json, Member.class);
                    remove(member);
                } catch (JsonSyntaxException e) {
                    LOGGER.error("Failed to parse Member from JSON: {}", json, e);
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

    public void remove(@NotNull Member member) {
        cache.invalidate(member);
        CacheUtil.remove(cacheByClanId, member.clanId(), v -> v.clanId().equals(member.clanId()));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.clanId().equals(member.clanId()));
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

    public record Member(@NotNull UUID clanId, int userId) {
    }
}
