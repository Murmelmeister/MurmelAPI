package de.murmelmeister.murmelapi.user.color;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.user.UserPrefixColorException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class UserPrefixColorProviderImpl implements UserPrefixColorProvider {
    private static final String TABLE_NAME = "user_prefix_colors";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (user_id, color_id, active)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                active = VALUES(active)
            RETURNING user_id, color_id, active, created_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE user_id = ? AND color_id = ?".formatted(TABLE_NAME);

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
    public @NotNull Optional<UserPrefixColor> findById(int userId, @NotNull String colorId) {
        return cache.getByKey(userId, colorId);
    }

    @Override
    public @NotNull Optional<UserPrefixColor> findActiveById(int userId) {
        return cache.getByUser(userId).stream()
                .filter(UserPrefixColor::active)
                .findFirst();
    }

    @Override
    public @NotNull @Unmodifiable List<UserPrefixColor> findByUserId(int userId) {
        return cache.getByUser(userId);
    }

    @Override
    public @NotNull Optional<UserPrefixColor> upsert(int userId, @NotNull String colorId, boolean active) {
        Objects.requireNonNull(colorId, "colorId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (colorId.length() > 100) throw new IllegalArgumentException("colorId cannot be longer than 100 characters");
        if (colorId.isBlank()) throw new IllegalArgumentException("colorId cannot be blank");

        Optional<UserPrefixColor> optExisting = cache.getByKey(userId, colorId);
        if (optExisting.isPresent())
            if (active == optExisting.get().active())
                return optExisting;

        UserPrefixColor color = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert UserPrefixColor (userId=" + userId + ", colorId=" + colorId + ")",
                () -> database.query(UPSERT_SQL, null, UserPrefixColorRowMapper::resultSet, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, colorId);
                    stmt.setBoolean(3, active);
                }),
                UserPrefixColorException::new
        );

        if (color == null) return Optional.empty();
        refreshProvider.fireSingle(single, new UserPrefixColorCache.ColorKey(userId, colorId));
        return Optional.of(color);
    }

    @Override
    public int delete(int userId, @NotNull String colorId) {
        Objects.requireNonNull(colorId, "colorId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (colorId.length() > 100) throw new IllegalArgumentException("colorId cannot be longer than 100 characters");
        if (colorId.isBlank()) throw new IllegalArgumentException("colorId cannot be blank");

        Optional<UserPrefixColor> optExisting = cache.getByKey(userId, colorId);
        if (optExisting.isEmpty()) return 0;
        UserPrefixColor existing = optExisting.get();

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete UserPrefixColor (userId=" + userId + ", colorId=" + colorId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, colorId);
                }),
                UserPrefixColorException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new UserPrefixColorCache.ColorKey(existing.userId(), existing.colorId()));
        return row;
    }
}
