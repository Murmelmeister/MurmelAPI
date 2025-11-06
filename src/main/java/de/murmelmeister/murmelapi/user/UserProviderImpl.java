package de.murmelmeister.murmelapi.user;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

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

    public UserProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "mojang_id VARCHAR(36) NOT NULL UNIQUE, " +
                "username VARCHAR(16) NOT NULL, " +
                "first_login DATETIME NULL, " +
                "system_user BOOLEAN NOT NULL DEFAULT FALSE, " +
                "debug_user BOOLEAN NOT NULL DEFAULT FALSE, " +
                "debug_enabled BOOLEAN NOT NULL DEFAULT FALSE, " +
                "language_id INT NOT NULL DEFAULT 1, " +
                "FOREIGN KEY (language_id) REFERENCES languages(id)");
        database.update("CREATE INDEX IF NOT EXISTS idx_users_username ON " + TABLE_NAME + " (username)");
    }

    public static void createConsoleUser(Database database) {
        String sql = "INSERT IGNORE INTO " + TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        database.update(sql, stmt -> {
            stmt.setInt(1, CONSOLE_USER_ID);
            stmt.setString(2, new UUID(0, 0).toString());
            stmt.setString(3, "Console");
            stmt.setNull(4, Types.TIMESTAMP);
            stmt.setBoolean(5, true);
            stmt.setBoolean(6, true);
            stmt.setBoolean(7, true);
            stmt.setInt(8, 1);
        });
    }

    @Override
    public void closeCache() {
        cache.close();
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public User findById(int userId) {
        return cache.getById(userId);
    }

    @Override
    public User findByMojangId(UUID uuid) {
        return cache.getByUUID(uuid);
    }

    @Override
    public User findByUsername(String username) {
        return cache.getByName(username);
    }

    @Override
    public List<User> findAll() {
        return cache.getCachedUsers().stream()
                .filter(user -> user.id() != CONSOLE_USER_ID)
                .toList();
    }

    @Override
    public List<UUID> findMojangIds() {
        return findAll().stream()
                .map(User::mojangId)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<String> findUsernames() {
        return findAll().stream()
                .map(User::username)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public User create(UUID uuid, String username) {
        if (uuid == null || username == null) return null;

        username = username.strip();
        if (username.isEmpty()) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (mojang_id, username) VALUES (?, ?)";
        String finalUsername = username;
        int id = (int) database.updateAndGetGeneratedKeys(sql, stmt -> {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, finalUsername);
        });
        if (id < 1) return null;

        User newUser = new User(id, uuid, username, null, false, false, false, 1);
        RefreshUtil.fireSingle(single, newUser.id());
        return newUser;
    }

    @Override
    public int delete(int userId) {
        if (userId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, stmt -> stmt.setInt(1, userId));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, userId);
        return row;
    }

    @Override
    public User update(int userId, String username, LocalDateTime firstLogin, boolean debugUser, boolean debugEnabled, int languageId) {
        if (userId < 1 || firstLogin == null || username == null || languageId < 1)
            return null;

        username = username.strip();
        if (username.isEmpty()) return null;

        User existing = cache.getById(userId);
        if (existing == null) return null;

        if (Objects.equals(username, existing.username()) &&
                Objects.equals(firstLogin, existing.firstLogin()) &&
                debugUser == existing.debugUser() &&
                debugEnabled == existing.debugEnabled() &&
                languageId == existing.languageId())
            return existing; // No changes, return existing user

        String sql = "UPDATE " + TABLE_NAME + " SET mojang_id = ?, username = ?, first_login = ?, debug_user = ?, debug_enabled = ?, language_id = ? WHERE id = ?";
        String finalUsername = username;
        int row = database.update(sql, stmt -> {
                    stmt.setString(1, existing.mojangId().toString());
                    stmt.setString(2, finalUsername);
                    stmt.setTimestamp(3, Timestamp.valueOf(firstLogin));
                    stmt.setBoolean(4, debugUser);
                    stmt.setBoolean(5, debugEnabled);
                    stmt.setInt(6, languageId);
                    stmt.setInt(7, userId);
                });
        if (row < 1) return null;

        User user = existing.withUpdateMeta(username, firstLogin, debugUser, debugEnabled, languageId);
        RefreshUtil.fireSingle(single, userId);
        return user;
    }
}
