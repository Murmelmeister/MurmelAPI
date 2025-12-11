package de.murmelmeister.murmelapi.user.session;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.net.InetAddress;
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
    public UserSession create(int userId, InetAddress inetAddress, String clientBrand, int protocolVersion) {
        if (userId < 1 || inetAddress == null) return null;

        UUID sessionId = UUID.randomUUID();
        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, user_id, ip_address, client_brand, protocol_version) VALUES (?, ?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, sessionId.toString());
            stmt.setInt(2, userId);
            stmt.setString(3, inetAddress.getHostAddress());
            stmt.setString(4, clientBrand);
            stmt.setInt(5, protocolVersion);
        });
        if (row < 1) return null;

        String selectSql = "SELECT login_time FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime loginTime = database.query(selectSql, null, resultSet ->
                        resultSet.getTimestamp("login_time").toLocalDateTime(),
                stmt -> stmt.setString(1, sessionId.toString()));
        if (loginTime == null) return null;

        UserSession session = new UserSession(sessionId, userId, loginTime, inetAddress, clientBrand, protocolVersion);
        RefreshUtil.fireSingle(single, session.id());
        return session;
    }

    @Override
    public int delete(UUID sessionId) {
        if (sessionId == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql,
                stmt -> stmt.setString(1, sessionId.toString()));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, sessionId);
        return row;
    }
}
