package de.murmelmeister.murmelapi.user.color;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public final class UserPrefixColorProviderImpl implements UserPrefixColorProvider {
    private static final String TABLE_NAME = "user_prefix_colors";

    private final Database database;
    private final UserPrefixColorCache cache;
    private final RefreshType all = RefreshType.USER_PREFIX_COLORS;
    private final RefreshType single = RefreshType.SINGLE_USER_PREFIX_COLOR;

    public UserPrefixColorProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserPrefixColorCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public UserPrefixColor findById(int userId, String colorId) {
        return cache.get(userId, colorId);
    }

    @Override
    public List<UserPrefixColor> findAll() {
        return cache.getAll();
    }

    @Override
    public UserPrefixColor create(int userId, String colorId, boolean active) {
        if (userId < 1 || colorId == null) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (user_id, color_id, active) VALUES (?, ?, ?)";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, colorId);
            stmt.setBoolean(3, active);
        });
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE user_id = ? AND color_id = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(), stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, colorId);
                });
        if (createdAt == null) return null;

        UserPrefixColor color = new UserPrefixColor(userId, colorId, active, createdAt);
        RefreshUtil.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return color;
    }

    @Override
    public int delete(int userId, String colorId) {
        if (userId < 1 || colorId == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ? AND color_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, colorId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return row;
    }

    @Override
    public UserPrefixColor update(int userId, String colorId, boolean active) {
        if (userId < 1 || colorId == null) return null;

        UserPrefixColor existing = cache.get(userId, colorId);
        if (existing == null) return null;

        if (active == existing.active()) return existing;
        String sql = "UPDATE " + TABLE_NAME + " SET active = ? WHERE user_id = ? AND color_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setBoolean(1, active);
            stmt.setInt(2, userId);
            stmt.setString(3, colorId);
        });
        if (row < 1) return null;

        UserPrefixColor updated = existing.withActive(active);
        RefreshUtil.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return updated;
    }
}
