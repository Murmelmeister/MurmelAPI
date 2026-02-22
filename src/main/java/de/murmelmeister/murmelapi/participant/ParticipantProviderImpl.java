package de.murmelmeister.murmelapi.participant;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.participant.ParticipantException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
import java.time.Duration;
import java.util.List;

public final class ParticipantProviderImpl implements ParticipantProvider {
    private static final String TABLE_NAME = "participants";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (group_id, user_id)
            VALUES (?, ?)
            RETURNING id, group_id, user_id
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ParticipantCache cache;
    private final RefreshType all = RefreshType.PARTICIPANTS;
    private final RefreshType single = RefreshType.SINGLE_PARTICIPANT;

    public ParticipantProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ParticipantCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable Participant findByGroupId(int groupId) {
        return cache.getById(groupId, null);
    }

    @Override
    public @Nullable Participant findByUserId(int userId) {
        return cache.getById(null, userId);
    }

    @Override
    public @NotNull @Unmodifiable List<Participant> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable Participant create(Integer groupId, Integer userId) {
        if (groupId == null && userId == null) throw new IllegalArgumentException("groupId and userId cannot be null");

        Participant participant = MurmelExceptionWrapper.dbWrap(
                "Failed to create Participant (groupId=" + groupId + ", userId=" + userId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.participant(),
                        stmt -> {
                            stmt.setObject(1, groupId, Types.INTEGER);
                            stmt.setObject(2, userId, Types.INTEGER);
                        }),
                ParticipantException::new
        );

        if (participant == null) return null;
        refreshProvider.fireSingle(single, participant);
        return participant;
    }

    @Override
    public int delete(Integer groupId, Integer userId) {
        if (groupId == null && userId == null) throw new IllegalArgumentException("groupId and userId cannot be null");

        Participant existing = cache.getById(groupId, userId);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Participant (groupId=" + groupId + ", userId=" + userId + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, existing.id())),
                ParticipantException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing);
        return row;
    }
}
