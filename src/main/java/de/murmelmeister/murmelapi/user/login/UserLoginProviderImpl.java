package de.murmelmeister.murmelapi.user.login;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class UserLoginProviderImpl implements UserLoginProvider {
    private static final String TABLE_NAME = "user_login";

    private final Database database;
    private final UserLoginCache cache;
    private final RefreshType all = RefreshType.USER_LOGINS;
    private final RefreshType single = RefreshType.SINGLE_USER_LOGIN;

    public UserLoginProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserLoginCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
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
    public List<UserLogin> findByIpAddress(InetAddress inetAddress) {
        return cache.getByIpAddress(inetAddress);
    }

    @Override
    public List<UserLogin> findAll() {
        return cache.getCachedLogins();
    }

    @Override
    public UserLogin create(UUID sessionId, int userId, LocalDateTime loginTime, InetAddress inetAddress, String clientBrand, int protocolVersion) {
        if (sessionId == null || userId < 1 || loginTime == null || inetAddress == null)
            return null;

        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, user_id, login_time, ip_address, client_brand, protocol_version) VALUES (?, ?, ?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, sessionId.toString());
            stmt.setInt(2, userId);
            stmt.setTimestamp(3, Timestamp.valueOf(loginTime));
            stmt.setString(4, inetAddress.getHostAddress());
            stmt.setString(5, clientBrand);
            stmt.setInt(6, protocolVersion);
        });
        if (row < 1) return null;

        String selectSql = "SELECT logout_time FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime logoutTime = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("logout_time").toLocalDateTime(),
                stmt -> stmt.setString(1, sessionId.toString()));
        if (logoutTime == null) return null;

        UserLogin login = new UserLogin(sessionId, userId, loginTime, logoutTime, inetAddress, clientBrand, protocolVersion);
        RefreshUtil.fireSingle(single, sessionId);
        return login;
    }

    @Override
    public UserLogin create(UserSession session) {
        if (session == null) return null;
        return create(session.id(), session.userId(), session.loginTime(),
                session.inetAddress(), session.clientBrand(), session.protocolVersion());
    }

    @Override
    public int delete(UUID id) {
        if (id == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql,
                stmt -> stmt.setString(1, id.toString()));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, id);
        return row;
    }
}
