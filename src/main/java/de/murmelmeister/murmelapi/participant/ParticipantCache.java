package de.murmelmeister.murmelapi.participant;

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

public class ParticipantCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP = "SELECT * FROM %s WHERE group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_USER = "SELECT * FROM %s WHERE user_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<Participant>> cacheByGroupId;
    private final LoadingCache<@NotNull Integer, Optional<Participant>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<Participant>> listCache;

    public ParticipantCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByGroupId = CacheUtil.buildCacheRefresh(this::loadByGroupId, cacheCapacity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.PARTICIPANTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PARTICIPANT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof Participant participant)
                remove(participant);
            else if (key instanceof String json) {
                try {
                    final Participant participant = gson.fromJson(json, Participant.class);

                    if (participant == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(participant);
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

    private @NotNull Optional<Participant> loadByGroupId(int groupId) {
        String sql = SELECT_BY_GROUP.formatted(tableName);
        Participant participant = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.participant(), stmt -> stmt.setInt(1, groupId));
        return Optional.ofNullable(participant);
    }

    private @NotNull Optional<Participant> loadByUserId(int userId) {
        String sql = SELECT_BY_USER.formatted(tableName);
        Participant participant = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.participant(), stmt -> stmt.setInt(1, userId));
        return Optional.ofNullable(participant);
    }

    private @NotNull List<Participant> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.participant());
    }

    public @Nullable Participant getById(Integer groupId, Integer userId) {
        if (groupId == null && userId == null) return null;
        Optional<Participant> optParticipant = groupId != null ? cacheByGroupId.get(groupId) : cacheByUserId.get(userId);
        return optParticipant != null && optParticipant.isPresent() ? optParticipant.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<Participant> getAll() {
        List<Participant> participants = listCache.get(ALL_KEY);
        if (participants == null || participants.isEmpty())
            return Collections.emptyList();
        return List.copyOf(participants);
    }

    public void remove(@NotNull Participant participant) {
        if (participant.groupId() != null) cacheByGroupId.invalidate(participant.groupId());
        if (participant.userId() != null) cacheByUserId.invalidate(participant.userId());
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == participant.id());
    }

    public void clear() {
        cacheByGroupId.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }
}
