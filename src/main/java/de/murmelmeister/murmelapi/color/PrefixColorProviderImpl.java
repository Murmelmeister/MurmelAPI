package de.murmelmeister.murmelapi.color;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PrefixColorProviderImpl implements PrefixColorProvider {
    private static final String TABLE_NAME = "prefix_colors";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PrefixColorCache cache;
    private final RefreshType all = RefreshType.PREFIX_COLORS;
    private final RefreshType single = RefreshType.SINGLE_PREFIX_COLOR;

    public PrefixColorProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PrefixColorCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
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
    public @NotNull List<PrefixColor> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable PrefixColor create(@NotNull String id, @NotNull String color, boolean animated, int createdBy) {
        if (createdBy < CONSOLE_USER_ID)
            return null;

        @Language("MariaDB")
        String sqlInsert = """
                INSERT INTO %s (id, color, animated, created_by)
                VALUES (?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(sqlInsert, stmt -> {
            stmt.setString(1, id);
            stmt.setString(2, color);
            stmt.setBoolean(3, animated);
            stmt.setInt(4, createdBy);
        });
        if (row == 0) return null;

        @Language("MariaDB")
        String sqlSelect = "SELECT created_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(sqlSelect, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setString(1, id));
        if (createdAt == null) return null;

        PrefixColor prefixColor = new PrefixColor(id, color, animated, createdAt, createdBy, null, null);
        refreshProvider.fireSingle(single, id);
        return prefixColor;
    }

    @Override
    public int delete(@NotNull String id) {
        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> stmt.setString(1, id));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, id);
        return row;
    }

    @Override
    public @Nullable PrefixColor update(@NotNull String id, @NotNull String color, boolean animated, int changedBy) {
        if (changedBy < CONSOLE_USER_ID)
            return null;

        PrefixColor existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(color, existing.color()) && animated == existing.animated())
            return existing;

        @Language("MariaDB")
        String sql = "UPDATE %s SET color = ?, animated = ?, changed_by = ? WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, color);
            stmt.setBoolean(2, animated);
            stmt.setInt(3, changedBy);
            stmt.setString(4, id);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> stmt.setString(1, id));
        if (changedAt == null) return null;

        PrefixColor prefixColor = PrefixColor.builder(existing)
                .color(color)
                .animated(animated)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, id);
        return prefixColor;
    }
}
