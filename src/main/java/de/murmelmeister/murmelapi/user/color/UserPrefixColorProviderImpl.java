package de.murmelmeister.murmelapi.user.color;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.user.UserPrefixColorException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class UserPrefixColorProviderImpl implements UserPrefixColorProvider {
    private static final String TABLE_NAME = "user_prefix_colors";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (user_id, color_id, active)
            VALUES (?, ?, ?)
            RETURNING user_id, color_id, active, created_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE user_id = ? AND color_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET active = ?
            WHERE user_id = ? AND color_id = ?
            """.formatted(TABLE_NAME);

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
        Objects.requireNonNull(colorId, "colorId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (colorId.length() > 100) throw new IllegalArgumentException("colorId cannot be longer than 100 characters");
        if (colorId.isBlank()) throw new IllegalArgumentException("colorId cannot be blank");

        UserPrefixColor color = MurmelExceptionWrapper.dbWrap(
                "Failed to create UserPrefixColor (userId=" + userId + ", colorId=" + colorId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.userPrefixColor(), stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, colorId);
                    stmt.setBoolean(3, active);
                }),
                UserPrefixColorException::new
        );

        if (color == null) return null;
        refreshProvider.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return color;
    }

    @Override
    public int delete(int userId, @NotNull String colorId) {
        Objects.requireNonNull(colorId, "colorId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (colorId.length() > 100) throw new IllegalArgumentException("colorId cannot be longer than 100 characters");
        if (colorId.isBlank()) throw new IllegalArgumentException("colorId cannot be blank");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete UserPrefixColor (userId=" + userId + ", colorId=" + colorId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, colorId);
                }),
                UserPrefixColorException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return row;
    }

    @Override
    public @Nullable UserPrefixColor update(int userId, @NotNull String colorId, boolean active) {
        Objects.requireNonNull(colorId, "colorId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (colorId.length() > 100) throw new IllegalArgumentException("colorId cannot be longer than 100 characters");
        if (colorId.isBlank()) throw new IllegalArgumentException("colorId cannot be blank");

        UserPrefixColor existing = cache.get(userId, colorId);
        if (existing == null) return null;

        if (active == existing.active()) return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update UserPrefixColor (userId=" + userId + ", colorId=" + colorId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setBoolean(1, active);
                    stmt.setInt(2, userId);
                    stmt.setString(3, colorId);
                }),
                UserPrefixColorException::new
        );
        if (row != 1) return null;

        UserPrefixColor updated = UserPrefixColor.builder(existing)
                .active(active)
                .build();
        refreshProvider.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return updated;
    }
}
