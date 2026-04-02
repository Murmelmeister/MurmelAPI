package de.murmelmeister.murmelapi.user.login;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.user.UserLoginException;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class UserLoginProviderImpl implements UserLoginProvider {
    private static final String TABLE_NAME = "user_login";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (id, user_id, login_time, ip_address, client_brand, protocol_version)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, user_id, login_time, logout_time, ip_address, client_brand, protocol_version
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserLoginCache cache;
    private final RefreshType all = RefreshType.USER_LOGINS;
    private final RefreshType single = RefreshType.SINGLE_USER_LOGIN;

    public UserLoginProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserLoginCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<UserLogin> findById(@NotNull UUID id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<UserLogin> findByUserId(int userId) {
        return cache.getByUserId(userId);
    }

    @Override
    public @NotNull @Unmodifiable List<UserLogin> findByIpAddress(@Nullable InetAddress inetAddress) {
        return cache.getByIpAddress(inetAddress);
    }

    @Override
    public @NotNull @Unmodifiable List<UserLogin> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull Optional<UserLogin> create(@NotNull UUID sessionId, int userId, @NotNull LocalDateTime loginTime, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(loginTime, "loginTime cannot be null");
        Objects.requireNonNull(inetAddress, "inetAddress cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (clientBrand != null && clientBrand.length() > 50)
            throw new IllegalArgumentException("clientBrand cannot be longer than 50 characters");

        UserLogin login = MurmelExceptionWrapper.dbWrap(
                "Failed to create UserLogin (sessionId=" + sessionId + ")",
                () -> database.query(CREATE_SQL, null, UserLoginRowMapper::resultSet, stmt -> {
                    stmt.setString(1, sessionId.toString());
                    stmt.setInt(2, userId);
                    stmt.setTimestamp(3, Timestamp.valueOf(loginTime));
                    stmt.setString(4, inetAddress.getHostAddress());
                    stmt.setString(5, clientBrand);
                    stmt.setInt(6, protocolVersion);
                }),
                UserLoginException::new
        );

        if (login == null) return Optional.empty();
        refreshProvider.fireSingle(single, new UserLoginCache.LoginKey(login.id(), login.userId(), login.inetAddress()));
        return Optional.of(login);
    }

    @Override
    public @NotNull Optional<UserLogin> create(@NotNull UserSession session) {
        Objects.requireNonNull(session, "session cannot be null");
        return create(session.id(), session.userId(), session.loginTime(),
                session.inetAddress(), session.clientBrand(), session.protocolVersion());
    }

    @Override
    public int delete(@NotNull UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");

        Optional<UserLogin> existingOpt = cache.getById(sessionId);
        if (existingOpt.isEmpty()) return 0;
        UserLogin existing = existingOpt.get();

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete (sessionId=" + sessionId + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setString(1, sessionId.toString())),
                UserLoginException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new UserLoginCache.LoginKey(existing.id(), existing.userId(), existing.inetAddress()));
        return row;
    }
}
