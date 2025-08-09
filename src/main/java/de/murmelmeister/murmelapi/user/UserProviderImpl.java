package de.murmelmeister.murmelapi.user;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

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
    public static final int CONSOLE_USER_ID = -1;
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
        database.update(sql, CONSOLE_USER_ID, new UUID(0, 0).toString(), "Console", null, true, true, true, 1);
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
        int id = (int) database.updateWithGeneratedKeys(sql, uuid.toString(), username);
        if (id < 1) return null;

        User newUser = new User(id, uuid, username, null, false, false, false, 1);
        RefreshUtil.fireSingle(single, newUser.id());
        cache.put(newUser);
        return newUser;
    }

    @Override
    public int delete(int userId) {
        if (userId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, userId);
        if (row < 1) return 0;

        cache.remove(userId);
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
        int row = database.update(sql, existing.mojangId().toString(), username, firstLogin, debugUser, debugEnabled, languageId, userId);
        if (row < 1) return null;

        User user = existing.withUpdateMeta(username, firstLogin, debugUser, debugEnabled, languageId);
        RefreshUtil.fireSingle(single, userId);
        cache.put(user);
        return user;
    }
}
