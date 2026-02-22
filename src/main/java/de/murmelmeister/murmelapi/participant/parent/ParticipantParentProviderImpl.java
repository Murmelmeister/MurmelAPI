package de.murmelmeister.murmelapi.participant.parent;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.participant.ParticipantParentException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ParticipantParentProviderImpl implements ParticipantParentProvider {
    private static final String TABLE_NAME = "participant_parents";

    @Language("MariaDB")
    private static final String ADD_SQL = """
            INSERT INTO %s (participant_id, parent_id. expires_at, created_by)
            VALUES (?, ?, ?, ?)
            RETURNING id, participant_id, parent_id, expires_at, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String REMOVE_SQL = "DELETE FROM %s WHERE participant_id = ? AND parent_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String CLEAR_SQL = "DELETE FROM %s WHERE participant_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET expires_at = ?,
                changed_by = ?
            WHERE participant_id = ? AND parent_id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE participant_id = ? AND parent_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String EXPIRES_SQL = "DELETE FROM %s WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ParticipantParentCache cache;
    private final RefreshType all = RefreshType.PARTICIPANT_PARENTS;
    private final RefreshType single = RefreshType.SINGLE_PARTICIPANT_PARENT;

    public ParticipantParentProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ParticipantParentCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable ParticipantParent findParent(int participantId, int parentId) {
        return cache.getByKey(participantId, parentId);
    }

    @Override
    public @NotNull @Unmodifiable List<ParticipantParent> findParents(int participantId) {
        return cache.getByParticipant(participantId);
    }

    @Override
    public @Nullable ParticipantParent add(int participantId, int parentId, long duration, int createdBy) {
        if (participantId < 1) throw new IllegalArgumentException("participantId must be >= 1");
        if (parentId < 1) throw new IllegalArgumentException("parentId must be >= 1");
        if (duration < -1) throw new IllegalArgumentException("duration must be >= -1");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        ParticipantParent parent = MurmelExceptionWrapper.dbWrap(
                "Failed to add ParticipantParent (participantId=" + participantId + ", parentId=" + parentId + ")",
                () -> database.query(ADD_SQL, null, ResultSetUtil.participantParent(), stmt -> {
                    stmt.setInt(1, participantId);
                    stmt.setInt(2, parentId);
                    stmt.setObject(3, expiresAt, Types.TIMESTAMP);
                    stmt.setInt(4, createdBy);
                }),
                ParticipantParentException::new
        );

        if (parent == null) return null;
        refreshProvider.fireSingle(single, new ParticipantParentCache.ParentKey(participantId, parentId));
        return parent;
    }

    @Override
    public int remove(int participantId, int parentId) {
        if (participantId < 1) throw new IllegalArgumentException("participantId must be >= 1");
        if (parentId < 1) throw new IllegalArgumentException("parentId must be >= 1");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to remove ParticipantParent (participantId=" + participantId + ", parentId=" + parentId + ")",
                () -> database.update(REMOVE_SQL, stmt -> {
                    stmt.setInt(1, participantId);
                    stmt.setInt(2, parentId);
                }),
                ParticipantParentException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new ParticipantParentCache.ParentKey(participantId, parentId));
        return row;
    }

    @Override
    public int clear(int participantId) {
        if (participantId < 1) throw new IllegalArgumentException("participantId must be >= 1");

        int rows = MurmelExceptionWrapper.dbWrap(
                "Failed to clear ParticipantParents (participantId=" + participantId + ")",
                () -> database.update(CLEAR_SQL, stmt -> stmt.setInt(1, participantId)),
                ParticipantParentException::new
        );

        if (rows < 1) return 0;
        refreshProvider.fireSingle(single, new ParticipantParentCache.ParentKey(participantId, null));
        return rows;
    }

    @Override
    public @Nullable ParticipantParent update(int participantId, int parentId, long duration, int changedBy) {
        if (participantId < 1) throw new IllegalArgumentException("participantId must be >= 1");
        if (parentId < 1) throw new IllegalArgumentException("parentId must be >= 1");
        if (duration < -1) throw new IllegalArgumentException("duration must be >= -1");
        if (changedBy < CONSOLE_USER_ID) throw new IllegalArgumentException("changedBy must be >= " + CONSOLE_USER_ID);

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        ParticipantParent existing = cache.getByKey(participantId, parentId);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update ParticipantParent (participantId=" + participantId + ", parentId=" + parentId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setObject(1, expiresAt, Types.TIMESTAMP);
                    stmt.setInt(2, changedBy);
                    stmt.setInt(3, participantId);
                    stmt.setInt(4, parentId);
                }),
                ParticipantParentException::new
        );
        if (row != 1) return null;

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to get changedAt for ParticipantParent (participantId=" + participantId + ", parentId=" + parentId + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> {
                            stmt.setInt(1, participantId);
                            stmt.setInt(2, parentId);
                        }),
                ParticipantParentException::new
        );
        if (changedAt == null) return null;

        ParticipantParent updated = ParticipantParent.builder(existing)
                .expiresAt(expiresAt)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new ParticipantParentCache.ParentKey(participantId, parentId));
        return updated;
    }

    @Override
    public int loadExpired() {
        List<ParticipantParentCache.ParentKey> expiredParents = cache.getAll().stream()
                .filter(ParticipantParent::isExpired)
                .map(parent -> new ParticipantParentCache.ParentKey(parent.participantId(), parent.parentId()))
                .toList();
        if (expiredParents.isEmpty()) return 0;

        int removed = MurmelExceptionWrapper.dbWrap(
                "Failed to remove expired ParticipantParents",
                () -> database.update(EXPIRES_SQL),
                ParticipantParentException::new
        );

        if (removed < 1) return 0;
        expiredParents.forEach(key -> refreshProvider.fireSingle(single, key));
        return removed;
    }
}
