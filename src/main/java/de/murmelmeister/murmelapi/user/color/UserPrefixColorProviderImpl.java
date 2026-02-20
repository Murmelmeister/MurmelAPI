package de.murmelmeister.murmelapi.user.color;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;

public final class UserPrefixColorProviderImpl implements UserPrefixColorProvider {
    private static final String TABLE_NAME = "user_prefix_colors";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserPrefixColorCache cache;
    private final RefreshType all = RefreshType.USER_PREFIX_COLORS;
    private final RefreshType single = RefreshType.SINGLE_USER_PREFIX_COLOR;

    public UserPrefixColorProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserPrefixColorCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable UserPrefixColor findById(int userId, @NotNull String colorId) {
        return cache.get(userId, colorId);
    }

    @Override
    public @NotNull @Unmodifiable List<UserPrefixColor> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable UserPrefixColor create(int userId, @NotNull String colorId, boolean active) {
        if (userId < 1) return null;

        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (user_id, color_id, active)
                VALUES (?, ?, ?)
                RETURNING user_id, color_id, active, created_at
                """.formatted(TABLE_NAME);
        UserPrefixColor color = database.query(sql, null, ResultSetUtil.userPrefixColor(), stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, colorId);
            stmt.setBoolean(3, active);
        });

        if (color == null) return null;
        refreshProvider.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return color;
    }

    @Override
    public int delete(int userId, @NotNull String colorId) {
        if (userId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE user_id = ? AND color_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, colorId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return row;
    }

    @Override
    public @Nullable UserPrefixColor update(int userId, @NotNull String colorId, boolean active) {
        if (userId < 1) return null;

        UserPrefixColor existing = cache.get(userId, colorId);
        if (existing == null) return null;

        if (active == existing.active()) return existing;
        @Language("MariaDB")
        String sql = "UPDATE %s SET active = ? WHERE user_id = ? AND color_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setBoolean(1, active);
            stmt.setInt(2, userId);
            stmt.setString(3, colorId);
        });
        if (row < 1) return null;

        UserPrefixColor updated = UserPrefixColor.builder(existing)
                .active(active)
                .build();
        refreshProvider.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return updated;
    }
}
