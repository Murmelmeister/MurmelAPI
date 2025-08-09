package de.murmelmeister.murmelapi.user.login;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class UserLoginProviderImpl implements UserLoginProvider {
    private static final String TABLE_NAME = "user_login_history";

    private final Database database;
    private final UserLoginCache cache;
    private final RefreshType all = RefreshType.USER_LOGINS;
    private final RefreshType single = RefreshType.SINGLE_USER_LOGIN;

    public UserLoginProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserLoginCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id VARCHAR(36) PRIMARY KEY, " +
                                         "user_id INT NOT NULL, " +
                                         "login_time DATETIME NOT NULL, " +
                                         "logout_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ip_address VARCHAR(45) NOT NULL, " +
                                         "client_version VARCHAR(100) NULL, " +
                                         "protocol_version VARCHAR(100) NULL, " +
                                         "FOREIGN KEY (user_id) REFERENCES users(id)"
        ); // Maybe add session_type or something similar in the future
        database.update("CREATE INDEX IF NOT EXISTS idx_user_id ON " + TABLE_NAME + " (user_id)");
        database.update("CREATE INDEX IF NOT EXISTS idx_ip_address ON " + TABLE_NAME + " (ip_address)");
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
    public UserLogin findById(UUID id) {
        return cache.getById(id);
    }

    @Override
    public List<UserLogin> findByUserId(int userId) {
        return cache.getByUserId(userId);
    }

    @Override
    public List<UserLogin> findByIpAddress(String ipAddress) {
        return cache.getByIpAddress(ipAddress);
    }

    @Override
    public List<UserLogin> findAllLogins() {
        return cache.getCachedLogins();
    }

    @Override
    public UserLogin create(UUID sessionId, int userId, LocalDateTime loginTime, String ipAddress, String clientVersion, String protocolVersion) {
        if (sessionId == null || userId < 1 || loginTime == null || ipAddress == null)
            return null;

        ipAddress = ipAddress.strip();
        if (ipAddress.isEmpty()) return null;

        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, user_id, login_time, ip_address, client_version, protocol_version) VALUES (?, ?, ?, ?, ?, ?)";
        int row = database.update(insertSql, sessionId.toString(), userId, loginTime, ipAddress, clientVersion, protocolVersion);
        if (row < 1) return null;

        String selectSql = "SELECT logout_time FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime logoutTime = database.query(selectSql, null, resultSet ->
                resultSet.getTimestamp("logout_time").toLocalDateTime(), sessionId.toString());
        if (logoutTime == null) return null;

        UserLogin login = new UserLogin(sessionId, userId, loginTime, logoutTime, ipAddress, clientVersion, protocolVersion);
        RefreshUtil.fireSingle(single, sessionId);
        cache.put(login);
        return login;
    }

    @Override
    public UserLogin create(UserSession session) {
        if (session == null) return null;
        return create(session.id(), session.userId(), session.loginTime(),
                session.ipAddress(), session.clientVersion(), session.protocolVersion());
    }

    @Override
    public int delete(UUID id) {
        if (id == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, id.toString());
        if (row < 1) return 0;

        cache.remove(id);
        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public UserLogin getLastLogin(int userId) {
        return findByUserId(userId).stream()
                .max(Comparator.comparing(UserLogin::loginTime))
                .orElse(null);
    }

    @Override
    public LocalDateTime getLastLoginTime(int userId) {
        return findByUserId(userId).stream()
                .map(UserLogin::loginTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}
