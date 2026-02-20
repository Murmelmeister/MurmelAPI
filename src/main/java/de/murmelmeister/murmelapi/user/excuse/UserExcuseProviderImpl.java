package de.murmelmeister.murmelapi.user.excuse;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class UserExcuseProviderImpl implements UserExcuseProvider {
    private static final String TABLE_NAME = "user_excuses";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserExcuseCache cache;
    private final RefreshType all = RefreshType.USER_EXCUSES;
    private final RefreshType single = RefreshType.SINGLE_USER_EXCUSE;

    public UserExcuseProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserExcuseCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable UserExcuse findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<UserExcuse> findByUserId(int userId) {
        return cache.getByUserId(userId);
    }

    @Override
    public @NotNull @Unmodifiable List<UserExcuse> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable UserExcuse create(int userId, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String reason, int createdBy) {
        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (user_id, start_at, end_at, reason, created_by)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, user_id, start_at, end_at, reason, created_at, created_by, changed_at, changed_by
                """.formatted(TABLE_NAME);
        UserExcuse userExcuse = database.query(sql, null, ResultSetUtil.userExcuse(), stmt -> {
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(startAt));
            stmt.setTimestamp(3, Timestamp.valueOf(endAt));
            stmt.setString(4, reason);
            stmt.setInt(5, createdBy);
        });

        if (userExcuse == null) return null;
        refreshProvider.fireSingle(single, userExcuse);
        return userExcuse;
    }

    @Override
    public @Nullable UserExcuse update(int id, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String reason, int changedBy) {
        UserExcuse existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(startAt, existing.startAt()) &&
                Objects.equals(endAt, existing.endAt()) &&
                Objects.equals(reason, existing.reason()))
            return existing;

        @Language("MariaDB")
        String sql = """
                UPDATE %s
                SET start_at = ?,
                    end_at = ?,
                    reason = ?,
                    changed_by = ?
                WHERE id = ?
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setTimestamp(1, Timestamp.valueOf(startAt));
            stmt.setTimestamp(2, Timestamp.valueOf(endAt));
            stmt.setString(3, reason);
            stmt.setInt(4, changedBy);
            stmt.setInt(5, id);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, id);
                });

        UserExcuse updated = UserExcuse.builder(existing)
                .startAt(startAt)
                .endAt(endAt)
                .reason(reason)
                .changedAt(changedAt)
                .changedBy(changedBy)
                .build();
        refreshProvider.fireSingle(single, updated);
        return updated;
    }
}
