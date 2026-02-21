package de.murmelmeister.murmelapi.color;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.color.PrefixColorException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PrefixColorProviderImpl implements PrefixColorProvider {
    private static final String TABLE_NAME = "prefix_colors";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (id, color, animated, created_by)
            VALUES (?, ?, ?, ?)
            RETURNING id, color, animated, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = "UPDATE %s SET color = ?, animated = ?, changed_by = ? WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (id, color, animated, created_by)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                color = VALUES(color),
                animated = VALUES(animated),
                changed_by = ?
            RETURNING id, color, animated, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PrefixColorCache cache;
    private final RefreshType all = RefreshType.PREFIX_COLORS;
    private final RefreshType single = RefreshType.SINGLE_PREFIX_COLOR;

    public PrefixColorProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PrefixColorCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable PrefixColor findById(@Nullable String id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<PrefixColor> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable PrefixColor create(@NotNull String id, @NotNull String color, boolean animated, int createdBy) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(color, "color cannot be null");
        if (id.length() > 100) throw new IllegalArgumentException("id cannot be longer than 100 characters");
        if (color.length() > 255) throw new IllegalArgumentException("color cannot be longer than 255 characters");
        if (id.isBlank()) throw new IllegalArgumentException("id cannot be blank");
        if (color.isBlank()) throw new IllegalArgumentException("color cannot be blank");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        PrefixColor prefixColor = MurmelExceptionWrapper.dbWrap(
                "Failed to create PrefixColor (id=" + id + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.prefixColor(), stmt -> {
                    stmt.setString(1, id);
                    stmt.setString(2, color);
                    stmt.setBoolean(3, animated);
                    stmt.setInt(4, createdBy);
                }),
                PrefixColorException::new
        );

        if (prefixColor == null) return null;
        refreshProvider.fireSingle(single, prefixColor);
        return prefixColor;
    }

    @Override
    public int delete(@NotNull String id) {
        Objects.requireNonNull(id, "id cannot be null");
        if (id.length() > 100) throw new IllegalArgumentException("id cannot be longer than 100 characters");
        if (id.isBlank()) throw new IllegalArgumentException("id cannot be blank");

        PrefixColor existing = cache.getById(id);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete PrefixColor (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setString(1, id)),
                PrefixColorException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing);
        return row;
    }

    @Override
    public @Nullable PrefixColor update(@NotNull String id, @NotNull String color, boolean animated, int changedBy) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(color, "color cannot be null");
        if (id.length() > 100) throw new IllegalArgumentException("id cannot be longer than 100 characters");
        if (color.length() > 255) throw new IllegalArgumentException("color cannot be longer than 255 characters");
        if (id.isBlank()) throw new IllegalArgumentException("id cannot be blank");
        if (color.isBlank()) throw new IllegalArgumentException("color cannot be blank");
        if (changedBy < CONSOLE_USER_ID) throw new IllegalArgumentException("changedBy must be >= " + CONSOLE_USER_ID);

        PrefixColor existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(color, existing.color()) && animated == existing.animated())
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update PrefixColor (id=" + id + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, color);
                    stmt.setBoolean(2, animated);
                    stmt.setInt(3, changedBy);
                    stmt.setString(4, id);
                }),
                PrefixColorException::new
        );
        if (row != 1) return null;

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to get changedAt for PrefixColor (id=" + id + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> stmt.setString(1, id)),
                PrefixColorException::new
        );
        if (changedAt == null) return null;

        PrefixColor prefixColor = PrefixColor.builder(existing)
                .color(color)
                .animated(animated)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, prefixColor);
        return prefixColor;
    }

    @Override
    public @Nullable PrefixColor upsert(@NotNull String id, @NotNull String color, boolean animated, int executorId) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(color, "color cannot be null");
        if (id.length() > 100) throw new IllegalArgumentException("id cannot be longer than 100 characters");
        if (color.length() > 255) throw new IllegalArgumentException("color cannot be longer than 255 characters");
        if (id.isBlank()) throw new IllegalArgumentException("id cannot be blank");
        if (color.isBlank()) throw new IllegalArgumentException("color cannot be blank");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        PrefixColor existing = cache.getById(id);
        if (existing != null
                && Objects.equals(color, existing.color())
                && animated == existing.animated())
            return existing;

        PrefixColor saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert PrefixColor (id=" + id + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.prefixColor(), stmt -> {
                    stmt.setString(1, id);
                    stmt.setString(2, color);
                    stmt.setBoolean(3, animated);
                    stmt.setInt(4, executorId);
                    stmt.setInt(5, executorId);
                }),
                PrefixColorException::new
        );

        if (saved == null) return null;
        refreshProvider.fireSingle(single, saved);
        return saved;
    }

    @Override
    public @Nullable PrefixColor upsert(@NotNull PrefixColor prefixColor, int executorId) {
        return upsert(prefixColor.id(), prefixColor.color(), prefixColor.animated(), executorId);
    }
}
