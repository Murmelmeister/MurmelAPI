package de.murmelmeister.murmelapi.user;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * UserProvider class to manage users in the database.
 * This class implements the User interface and provides methods to interact with user data.
 */
public final class UserProviderImpl implements UserProvider {
    private static final String TABLE_NAME = "users";

    private final Database database;
    private final UserCache cache;
    private final RefreshType all = RefreshType.USERS;
    private final RefreshType single = RefreshType.SINGLE_USER;

    public UserProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public @Nullable User findById(int userId) {
        return cache.getById(userId);
    }

    @Override
    public @Nullable User findByMojangId(@Nullable UUID uuid) {
        return cache.getByUUID(uuid);
    }

    @Override
    public @Nullable User findByUsername(@Nullable String username) {
        return cache.getByName(username);
    }

    @Override
    public @NotNull List<User> findAll() {
        return cache.getCachedUsers();
    }

    @Override
    public @NotNull List<UUID> findMojangIds() {
        return findAll().stream()
                .map(User::mojangId)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public @NotNull List<String> findUsernames() {
        return findAll().stream()
                .map(User::username)
                .toList();
    }

    @Override
    public @Nullable User create(@NotNull UUID uuid, @NotNull String username) {
        String normalizedUsername = StringUtil.normalize(username);
        if (normalizedUsername == null) return null;

        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (mojang_id, username)
                VALUES (?, ?)
                """.formatted(TABLE_NAME);
        int id = (int) database.updateAndGetGeneratedKeys(sql, stmt -> {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, normalizedUsername);
        });
        if (id < 1) return null;

        User newUser = new User(id, uuid, normalizedUsername, null, false, false, false, 1);
        RefreshUtil.fireSingle(single, newUser.id());
        return newUser;
    }

    @Override
    public int delete(int userId) {
        if (userId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> stmt.setInt(1, userId));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, userId);
        return row;
    }

    @Override
    public @Nullable User update(int userId, @NotNull String username, @Nullable LocalDateTime firstLogin, boolean debugUser, boolean debugEnabled, int languageId) {
        String normalizedUsername = StringUtil.normalize(username);
        if (userId < 1 || normalizedUsername == null || languageId < 1)
            return null;

        User existing = cache.getById(userId);
        if (existing == null) return null;

        if (Objects.equals(normalizedUsername, existing.username()) &&
                Objects.equals(firstLogin, existing.firstLogin()) &&
                debugUser == existing.debugUser() &&
                debugEnabled == existing.debugEnabled() &&
                languageId == existing.languageId())
            return existing; // No changes, return existing user

        @Language("MariaDB")
        String sql = """
                UPDATE %s
                SET username = ?,
                    first_login = ?,
                    debug_user = ?,
                    debug_enabled = ?,
                    language_id = ?
                WHERE id = ?
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, normalizedUsername);
            stmt.setTimestamp(2, firstLogin != null ? Timestamp.valueOf(firstLogin) : null);
            stmt.setBoolean(3, debugUser);
            stmt.setBoolean(4, debugEnabled);
            stmt.setInt(5, languageId);
            stmt.setInt(6, userId);
        });
        if (row < 1) return null;

        User user = User.builder(existing)
                .username(normalizedUsername)
                .firstLogin(firstLogin)
                .debugUser(debugUser)
                .debugEnabled(debugEnabled)
                .languageId(languageId)
                .build();
        RefreshUtil.fireSingle(single, userId);
        return user;
    }
}
