package de.murmelmeister.murmelapi.user.session;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.user.UserException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class UserSessionProviderImpl implements UserSessionProvider {
    private static final String TABLE_NAME = "user_session";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (id, user_id, ip_address, client_brand, protocol_version)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, user_id, login_time, ip_address, client_brand, protocol_version
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

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
    public @NotNull @Unmodifiable List<UserSession> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable UserSession create(int userId, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion) {
        Objects.requireNonNull(inetAddress, "inetAddress cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (clientBrand != null && clientBrand.length() > 50)
            throw new IllegalArgumentException("clientBrand cannot be longer than 50 characters");

        UUID sessionId = UUID.randomUUID();
        UserSession session = MurmelExceptionWrapper.dbWrap(
                "Failed to create UserSession (userId=" + userId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.userSession(), stmt -> {
                    stmt.setString(1, sessionId.toString());
                    stmt.setInt(2, userId);
                    stmt.setString(3, inetAddress.getHostAddress());
                    stmt.setString(4, clientBrand);
                    stmt.setInt(5, protocolVersion);
                }),
                UserException::new
        );

        if (session == null) return null;
        refreshProvider.fireSingle(single, session);
        return session;
    }

    @Override
    public int delete(@NotNull UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");

        UserSession existing = findById(sessionId);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete UserSession (sessionId=" + sessionId + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setString(1, sessionId.toString())),
                UserException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing);
        return row;
    }
}
