package de.murmelmeister.murmelapi.color;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PrefixColorProviderImpl implements PrefixColorProvider {
    private static final String TABLE_NAME = "prefix_colors";

    private final Database database;
    private final PrefixColorCache cache;
    private final RefreshType all = RefreshType.PREFIX_COLORS;
    private final RefreshType single = RefreshType.SINGLE_PREFIX_COLOR;

    public PrefixColorProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new PrefixColorCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public PrefixColor findById(String id) {
        return cache.getById(id);
    }

    @Override
    public List<PrefixColor> findAll() {
        return cache.getAll();
    }

    @Override
    public PrefixColor create(String id, String color, boolean animated, int createdBy) {
        if (id == null || color == null || createdBy < CONSOLE_USER_ID)
            return null;

        String sqlInsert = "INSERT INTO " + TABLE_NAME + " (id, color, animated, created_by) VALUES (?, ?, ?, ?)";
        int row = database.update(sqlInsert, stmt -> {
            stmt.setString(1, id);
            stmt.setString(2, color);
            stmt.setBoolean(3, animated);
            stmt.setInt(4, createdBy);
        });
        if (row == 0) return null;

        String sqlSelect = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(sqlSelect, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setString(1, id));
        if (createdAt == null) return null;

        PrefixColor prefixColor = new PrefixColor(id, color, animated, createdAt, createdBy, null, null);
        RefreshUtil.fireSingle(single, id);
        return prefixColor;
    }

    @Override
    public int delete(String id) {
        if (id == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, stmt -> stmt.setString(1, id));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public PrefixColor update(String id, String color, boolean animated, int changedBy) {
        if (id == null || color == null || changedBy < CONSOLE_USER_ID)
            return null;

        PrefixColor existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(color, existing.color()) && animated == existing.animated())
            return existing;

        String sql = "UPDATE " + TABLE_NAME + " SET color = ?, animated = ?, changed_by = ? WHERE id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, color);
            stmt.setBoolean(2, animated);
            stmt.setInt(3, changedBy);
            stmt.setString(4, id);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> stmt.setString(1, id));
        if (changedAt == null) return null;

        PrefixColor prefixColor = existing.withUpdateMeta(color, animated, changedAt, changedBy);
        RefreshUtil.fireSingle(single, id);
        return prefixColor;
    }
}
