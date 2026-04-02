package de.murmelmeister.murmelapi.user.excuse;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.user.UserExcuseException;
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
import java.util.Optional;

final class UserExcuseProviderImpl implements UserExcuseProvider {
    private static final String TABLE_NAME = "user_excuses";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (user_id, start_at, end_at, reason, created_by)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, user_id, start_at, end_at, reason, created_at, created_by, changed_at, changed_by
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET start_at = ?,
                end_at = ?,
                reason = ?,
                changed_by = ?
            WHERE id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);

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
    public @NotNull Optional<UserExcuse> findById(int id) {
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
    public @NotNull Optional<UserExcuse> create(int userId, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String reason, int createdBy) {
        Objects.requireNonNull(startAt, "startAt cannot be null");
        Objects.requireNonNull(endAt, "endAt cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (createdBy < 1) throw new IllegalArgumentException("createdBy must be >= 1");
        if (startAt.isAfter(endAt)) throw new IllegalArgumentException("startAt cannot be after endAt");
        if (reason != null && reason.isBlank()) throw new IllegalArgumentException("reason cannot be blank");

        UserExcuse userExcuse = MurmelExceptionWrapper.dbWrap(
                "Failed to create UserExcuse (userId=" + userId + ")",
                () -> database.query(CREATE_SQL, null, UserExcuseRowMapper::resultSet, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setTimestamp(2, Timestamp.valueOf(startAt));
                    stmt.setTimestamp(3, Timestamp.valueOf(endAt));
                    stmt.setString(4, reason);
                    stmt.setInt(5, createdBy);
                }),
                UserExcuseException::new
        );

        if (userExcuse == null) return Optional.empty();
        refreshProvider.fireSingle(single, new UserExcuseCache.UserKey(userExcuse.id(), userExcuse.userId()));
        return Optional.of(userExcuse);
    }

    @Override
    public @NotNull Optional<UserExcuse> update(int id, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String reason, int changedBy) {
        Objects.requireNonNull(startAt, "startAt cannot be null");
        Objects.requireNonNull(endAt, "endAt cannot be null");
        if (startAt.isAfter(endAt)) throw new IllegalArgumentException("startAt cannot be after endAt");
        if (reason != null && reason.isBlank()) throw new IllegalArgumentException("reason cannot be blank");
        if (changedBy < 1) throw new IllegalArgumentException("changedBy must be >= 1");

        Optional<UserExcuse> existingOpt = cache.getById(id);
        if (existingOpt.isEmpty()) return Optional.empty();
        UserExcuse existing = existingOpt.get();

        if (Objects.equals(startAt, existing.startAt()) &&
                Objects.equals(endAt, existing.endAt()) &&
                Objects.equals(reason, existing.reason()))
            return existingOpt;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update UserExcuse (id=" + id + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setTimestamp(1, Timestamp.valueOf(startAt));
                    stmt.setTimestamp(2, Timestamp.valueOf(endAt));
                    stmt.setString(3, reason);
                    stmt.setInt(4, changedBy);
                    stmt.setInt(5, id);
                }),
                UserExcuseException::new
        );
        if (row != 1) return Optional.empty();

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to get changedAt for UserExcuse (id=" + id + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> {
                            stmt.setInt(1, id);
                        }),
                UserExcuseException::new
        );
        if (changedAt == null) return Optional.empty();

        UserExcuse updated = existing.builder()
                .startAt(startAt)
                .endAt(endAt)
                .reason(reason)
                .changedAt(changedAt)
                .changedBy(changedBy)
                .build();
        refreshProvider.fireSingle(single, new UserExcuseCache.UserKey(updated.id(), updated.userId()));
        return Optional.of(updated);
    }
}
