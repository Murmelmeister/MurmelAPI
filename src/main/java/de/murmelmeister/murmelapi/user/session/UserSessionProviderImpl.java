package de.murmelmeister.murmelapi.user.session;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class UserSessionProviderImpl implements UserSessionProvider {
    private static final String TABLE_NAME = "user_session";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserSessionCache cache;
    private final RefreshType all = RefreshType.USER_SESSIONS;
    private final RefreshType single = RefreshType.SINGLE_USER_SESSION;

    public UserSessionProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserSessionCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable UserSession findById(@Nullable UUID sessionId) {
        return cache.getById(sessionId);
    }

    @Override
    public @Nullable UserSession findByUserId(int userId) {
        return cache.getByUserId(userId);
    }

    @Override
    public @NotNull List<UserSession> findAll() {
        return cache.getCachedSessions();
    }

    @Override
    public @Nullable UserSession create(int userId, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion) {
        if (userId < 1) return null;

        UUID sessionId = UUID.randomUUID();
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (id, user_id, ip_address, client_brand, protocol_version)
                VALUES (?, ?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, sessionId.toString());
            stmt.setInt(2, userId);
            stmt.setString(3, inetAddress.getHostAddress());
            stmt.setString(4, clientBrand);
            stmt.setInt(5, protocolVersion);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT login_time FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime loginTime = database.query(selectSql, null, resultSet ->
                        resultSet.getTimestamp("login_time").toLocalDateTime(),
                stmt -> stmt.setString(1, sessionId.toString()));
        if (loginTime == null) return null;

        UserSession session = new UserSession(sessionId, userId, loginTime, inetAddress, clientBrand, protocolVersion);
        refreshProvider.fireSingle(single, session);
        return session;
    }

    @Override
    public int delete(@Nullable UUID sessionId) {
        if (sessionId == null) return 0;

        UserSession existing = findById(sessionId);
        if (existing == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql,
                stmt -> stmt.setString(1, sessionId.toString()));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, existing);
        return row;
    }
}
