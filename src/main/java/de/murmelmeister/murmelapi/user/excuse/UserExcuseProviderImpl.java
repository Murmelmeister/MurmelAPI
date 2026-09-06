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

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class UserExcuseProviderImpl implements UserExcuseProvider {
    private static final String TABLE_NAME = "user_excuses";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (user_id, start_date, extra_days, reason, created_by)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, user_id, start_date, extra_days, reason, created_at, created_by, changed_at, changed_by
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET start_date = ?,
                extra_days = ?,
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
    public @NotNull Optional<UserExcuse> create(int userId, @NotNull LocalDate startDate, int extraDays, @Nullable String reason, int createdBy) {
        Objects.requireNonNull(startDate, "startDate cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (createdBy < 1) throw new IllegalArgumentException("createdBy must be >= 1");
        if (extraDays < 0) throw new IllegalArgumentException("extraDays must be >= 0");
        if (reason != null && reason.isBlank()) throw new IllegalArgumentException("reason cannot be blank");

        UserExcuse userExcuse = MurmelExceptionWrapper.dbWrap(
                "Failed to create UserExcuse (userId=" + userId + ")",
                () -> database.query(CREATE_SQL, null, UserExcuseRowMapper::resultSet, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setDate(2, Date.valueOf(startDate));
                    stmt.setInt(3, extraDays);
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
    public @NotNull Optional<UserExcuse> update(int id, @NotNull LocalDate startDate, int extraDays, @Nullable String reason, int changedBy) {
        Objects.requireNonNull(startDate, "startDate cannot be null");
        if (extraDays < 0) throw new IllegalArgumentException("extraDays must be >= 0");
        if (reason != null && reason.isBlank()) throw new IllegalArgumentException("reason cannot be blank");
        if (changedBy < 1) throw new IllegalArgumentException("changedBy must be >= 1");

        Optional<UserExcuse> existingOpt = cache.getById(id);
        if (existingOpt.isEmpty()) return Optional.empty();
        UserExcuse existing = existingOpt.get();

        if (Objects.equals(startDate, existing.startDate()) &&
                extraDays == existing.extraDays() &&
                Objects.equals(reason, existing.reason()))
            return existingOpt;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update UserExcuse (id=" + id + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setDate(1, Date.valueOf(startDate));
                    stmt.setInt(2, extraDays);
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
                .startDate(startDate)
                .extraDays(extraDays)
                .reason(reason)
                .changedAt(changedAt)
                .changedBy(changedBy)
                .build();
        refreshProvider.fireSingle(single, new UserExcuseCache.UserKey(updated.id(), updated.userId()));
        return Optional.of(updated);
    }
}
