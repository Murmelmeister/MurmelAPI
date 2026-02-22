package de.murmelmeister.murmelapi.participant.parent;

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

import java.sql.Types;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ParticipantParentCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantParentCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE participant_id = ? AND parent_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_PARTICIPANT = "SELECT * FROM %s WHERE participant_id = ?";
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ParentKey, Optional<ParticipantParent>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<ParticipantParent>> cacheByParticipant;
    private final LoadingCache<@NotNull String, List<ParticipantParent>> cacheAll;

    public ParticipantParentCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByParticipant = CacheUtil.buildCacheRefresh(this::loadByParticipant, cacheCapacity, refreshInterval);
        this.cacheAll = CacheUtil.buildCacheRefresh(key -> loadAll(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.PARTICIPANT_PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PARTICIPANT_PARENT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof ParentKey parentKey)
                remove(parentKey);
            else if (key instanceof String json) {
                try {
                    final ParentKey parentKey = gson.fromJson(json, ParentKey.class);

                    if (parentKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(parentKey);
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

    public @NotNull Optional<ParticipantParent> loadByKey(ParentKey parentKey) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        ParticipantParent participantParent = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.participantParent(),
                stmt -> {
                    stmt.setInt(1, parentKey.participantId());
                    stmt.setObject(2, parentKey.parentId(), Types.INTEGER);
                });
        return Optional.ofNullable(participantParent);
    }

    private @NotNull List<ParticipantParent> loadByParticipant(int participantId) {
        String sql = SELECT_BY_PARTICIPANT.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.participantParent(),
                stmt -> stmt.setInt(1, participantId));
    }

    private @NotNull List<ParticipantParent> loadAll() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.participantParent());
    }

    public @Nullable ParticipantParent getByKey(int participantId, int parentId) {
        Optional<ParticipantParent> optParent = cacheByKey.get(new ParentKey(participantId, parentId));
        return optParent != null && optParent.isPresent() ? optParent.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<ParticipantParent> getByParticipant(int participantId) {
        List<ParticipantParent> parents = cacheByParticipant.get(participantId);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public @NotNull @Unmodifiable List<ParticipantParent> getAll() {
        List<ParticipantParent> parents = cacheAll.get(ALL_KEY);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public void remove(@NotNull ParentKey parentKey) {
        cacheByKey.invalidate(parentKey);
        cacheByParticipant.invalidate(parentKey.participantId());
        CacheUtil.remove(cacheAll, ALL_KEY, v -> v.participantId() == parentKey.participantId());
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByParticipant.invalidateAll();
        cacheAll.invalidateAll();
    }

    public record ParentKey(int participantId, @Nullable Integer parentId) {
    }
}
