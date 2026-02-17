package de.murmelmeister.murmelapi.user.login;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class UserLoginProviderImpl implements UserLoginProvider {
    private static final String TABLE_NAME = "user_login";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserLoginCache cache;
    private final RefreshType all = RefreshType.USER_LOGINS;
    private final RefreshType single = RefreshType.SINGLE_USER_LOGIN;

    public UserLoginProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserLoginCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable UserLogin findById(@Nullable UUID id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull List<UserLogin> findByUserId(int userId) {
        return cache.getByUserId(userId);
    }

    @Override
    public @NotNull List<UserLogin> findByIpAddress(@Nullable InetAddress inetAddress) {
        return cache.getByIpAddress(inetAddress);
    }

    @Override
    public @NotNull List<UserLogin> findAll() {
        return cache.getCachedLogins();
    }

    @Override
    public UserLogin create(@NotNull UUID sessionId, int userId, @NotNull LocalDateTime loginTime, @NotNull InetAddress inetAddress, String clientBrand, int protocolVersion) {
        if (userId < 1)
            return null;

        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (id, user_id, login_time, ip_address, client_brand, protocol_version)
                VALUES (?, ?, ?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, sessionId.toString());
            stmt.setInt(2, userId);
            stmt.setTimestamp(3, Timestamp.valueOf(loginTime));
            stmt.setString(4, inetAddress.getHostAddress());
            stmt.setString(5, clientBrand);
            stmt.setInt(6, protocolVersion);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT logout_time FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime logoutTime = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("logout_time").toLocalDateTime(),
                stmt -> stmt.setString(1, sessionId.toString()));
        if (logoutTime == null) return null;

        UserLogin login = new UserLogin(sessionId, userId, loginTime, logoutTime, inetAddress, clientBrand, protocolVersion);
        refreshProvider.fireSingle(single, login);
        return login;
    }

    @Override
    public @Nullable UserLogin create(@NotNull UserSession session) {
        return create(session.id(), session.userId(), session.loginTime(),
                session.inetAddress(), session.clientBrand(), session.protocolVersion());
    }

    @Override
    public int delete(@Nullable UUID id) {
        if (id == null) return 0;

        UserLogin existing = cache.getById(id);
        if (existing == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql,
                stmt -> stmt.setString(1, id.toString()));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, existing);
        return row;
    }
}
