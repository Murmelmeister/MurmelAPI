package de.murmelmeister.murmelapi.user.session;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class UserSessionProviderImpl implements UserSessionProvider {
    private static final String TABLE_NAME = "user_session";

    private final Database database;
    private final UserSessionCache cache;
    private final RefreshType all = RefreshType.USER_SESSIONS;
    private final RefreshType single = RefreshType.SINGLE_USER_SESSION;

    public UserSessionProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserSessionCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id VARCHAR(36) PRIMARY KEY, " +
                                         "user_id INT NOT NULL UNIQUE, " +
                                         "login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ip_address VARCHAR(45) NOT NULL, " +
                                         "client_version VARCHAR(100) NULL, " +
                                         "protocol_version VARCHAR(100) NULL, " +
                                         "FOREIGN KEY (user_id) REFERENCES users(id)"
        ); // Maybe session_type in the future?
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
    public UserSession findById(UUID sessionId) {
        return cache.getById(sessionId);
    }

    @Override
    public UserSession findByUserId(int userId) {
        return cache.getByUserId(userId);
    }

    @Override
    public List<UserSession> findAll() {
        return cache.getCachedSessions();
    }

    @Override
    public List<UserSession> findUserSessions(int userId) {
        return findAll().stream()
                .filter(session -> session.userId() == userId)
                .toList();
    }

    @Override
    public UserSession create(int userId, String ipAddress, String clientVersion, String protocolVersion) {
        if (userId < 1 || ipAddress == null) return null;

        ipAddress = ipAddress.strip();
        if (ipAddress.isEmpty()) return null;

        UUID sessionId = UUID.randomUUID();
        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, user_id, ip_address, client_version, protocol_version) VALUES (?, ?, ?, ?, ?)";
        int row = database.update(insertSql, sessionId.toString(), userId, ipAddress, clientVersion, protocolVersion);
        if (row < 1) return null;

        String selectSql = "SELECT login_time FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime loginTime = database.query(selectSql, null, resultSet ->
                resultSet.getTimestamp("login_time").toLocalDateTime(), sessionId.toString());
        if (loginTime == null) return null;

        UserSession session = new UserSession(sessionId, userId, loginTime, ipAddress, clientVersion, protocolVersion);
        RefreshUtil.fireSingle(single, session.id());
        cache.put(session);
        return session;
    }

    @Override
    public int delete(UUID sessionId) {
        if (sessionId == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, sessionId.toString());
        if (row < 1) return 0;

        cache.remove(sessionId);
        RefreshUtil.fireSingle(single, sessionId);
        return row;
    }

    @Override
    public boolean isOnline(int userId) {
        if (userId < 1) return false;
        return findByUserId(userId) != null;
    }
}
