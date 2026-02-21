package de.murmelmeister.murmelapi.user;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.user.UserException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
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

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (mojang_id, username)
            VALUES (?, ?)
            RETURNING id, mojang_id, username, first_login, system_user, debug_user, debug_enabled, language_id
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET username = ?,
                first_login = ?,
                debug_user = ?,
                debug_enabled = ?,
                language_id = ?
            WHERE id = ?
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserCache cache;
    private final RefreshType all = RefreshType.USERS;
    private final RefreshType single = RefreshType.SINGLE_USER;

    public UserProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
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
    public @NotNull @Unmodifiable List<User> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull @Unmodifiable List<UUID> findMojangIds() {
        return findAll().stream()
                .map(User::mojangId)
                .toList();
    }

    @Override
    public @NotNull @Unmodifiable List<String> findUsernames() {
        return findAll().stream()
                .map(User::username)
                .toList();
    }

    @Override
    public @Nullable User create(@NotNull UUID uuid, @NotNull String username) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        Objects.requireNonNull(username, "username must not be null");

        String normalizedUsername = StringUtil.normalize(username);
        if (normalizedUsername == null || normalizedUsername.isBlank())
            throw new IllegalArgumentException("username must not be blank");

        User user = MurmelExceptionWrapper.dbWrap(
                "Failed to create User (uuid=" + uuid + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.user(), stmt -> {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, normalizedUsername);
                }),
                UserException::new
        );

        if (user == null) return null;
        refreshProvider.fireSingle(single, user);
        return user;
    }

    @Override
    public int delete(int userId) {
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");

        User existing = cache.getById(userId);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete User (id=" + userId + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, userId)),
                UserException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing);
        return row;
    }

    @Override
    public @Nullable User update(int userId, @NotNull String username, @Nullable LocalDateTime firstLogin, boolean debugUser, boolean debugEnabled, int languageId) {
        Objects.requireNonNull(username, "username must not be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (languageId < 1) throw new IllegalArgumentException("languageId must be >= 1");

        String normalizedUsername = StringUtil.normalize(username);
        if (normalizedUsername == null || normalizedUsername.isBlank())
            throw new IllegalArgumentException("username must not be blank");

        User existing = cache.getById(userId);
        if (existing == null) return null;

        if (Objects.equals(normalizedUsername, existing.username()) &&
                Objects.equals(firstLogin, existing.firstLogin()) &&
                debugUser == existing.debugUser() &&
                debugEnabled == existing.debugEnabled() &&
                languageId == existing.languageId())
            return existing; // No changes, return existing user

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update User (id=" + userId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, normalizedUsername);
                    stmt.setObject(2, firstLogin, Types.TIMESTAMP);
                    stmt.setBoolean(3, debugUser);
                    stmt.setBoolean(4, debugEnabled);
                    stmt.setInt(5, languageId);
                    stmt.setInt(6, userId);
                }),
                UserException::new
        );
        if (row != 1) return null;

        User user = User.builder(existing)
                .username(normalizedUsername)
                .firstLogin(firstLogin)
                .debugUser(debugUser)
                .debugEnabled(debugEnabled)
                .languageId(languageId)
                .build();
        refreshProvider.fireSingle(single, user);
        return user;
    }
}
