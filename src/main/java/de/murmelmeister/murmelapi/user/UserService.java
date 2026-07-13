package de.murmelmeister.murmelapi.user;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.user.UserException;
import de.murmelmeister.murmelapi.exceptions.user.UserLoginException;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAuditProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUserProvider;
import de.murmelmeister.murmelapi.user.excuse.UserExcuseProvider;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.murmelapi.user.stats.UserStatsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public record UserService(
        @NotNull Database database,
        @NotNull UserProvider userProvider,
        @NotNull UserStatsProvider statsProvider,
        @NotNull UserLoginProvider loginProvider,
        @NotNull UserSessionProvider sessionProvider,
        @NotNull UserExcuseProvider userExcuseProvider,
        @NotNull PunishmentUserProvider punishUserProvider,
        @NotNull PunishmentAuditProvider punishAuditProvider
) {
    public UserService {
        Objects.requireNonNull(database, "database must not be null");
        Objects.requireNonNull(userProvider, "userProvider must not be null");
        Objects.requireNonNull(statsProvider, "statsProvider must not be null");
        Objects.requireNonNull(loginProvider, "loginProvider must not be null");
        Objects.requireNonNull(sessionProvider, "sessionProvider must not be null");
        Objects.requireNonNull(userExcuseProvider, "userExcuseProvider must not be null");
        Objects.requireNonNull(punishUserProvider, "punishUserProvider must not be null");
        Objects.requireNonNull(punishAuditProvider, "punishAuditProvider must not be null");
    }

    public void startSession(int userId, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion) {
        Objects.requireNonNull(inetAddress, "inetAddress cannot be null");
        if (userId < 1)
            throw new IllegalArgumentException("User ID must be >= 1");

        // Recover a leftover session (e.g. after a crash) before opening a new one.
        sessionProvider.findByUserId(userId).ifPresent(this::endSession);
        sessionProvider.create(userId, inetAddress, clientBrand, protocolVersion);
    }

    public void closeSession(int userId) {
        if (userId < 1)
            throw new IllegalArgumentException("userId must be >= 1");

        sessionProvider.findByUserId(userId).ifPresent(this::endSession);
    }

    /**
     * Ends a session: archives it into the login history and removes the ephemeral session row.
     * <p>
     * Both writes happen inside a single transaction via the {@code end_user_session} stored procedure
     * (called by {@link UserLoginProvider#endSession(int)}).
     * Either both apply or neither does, so a crash between the two can never leave the session
     * archived-but-not-deleted (duplicate history) or deleted-but-not-archived (lost history).
     * <p>
     * The caches that {@code create}/{@code delete} would normally invalidate are refreshed afterwards,
     * once the transaction has committed.
     */
    private void endSession(@NotNull UserSession session) {
        int userId = session.userId();
        if (loginProvider.endSession(userId).isEmpty())
            throw new UserLoginException("Failed to end session for user ID: " + userId);
        sessionProvider.refreshSingle(session.id(), userId);
    }

    public @NotNull User join(@NotNull UUID uuid, @NotNull String username) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        Objects.requireNonNull(username, "username must not be null");

        User user = userProvider.findByMojangId(uuid)
                .orElseGet(() -> userProvider.create(uuid, username)
                        .orElseThrow(() -> new UserException("Failed to create user with UUID: " + uuid)));

        LocalDateTime firstLogin = user.firstLogin();
        LocalDateTime resolvedFirstLogin = firstLogin == null ? LocalDateTime.now() : firstLogin;
        boolean requiresUpdate = !user.username().equals(username) || !Objects.equals(firstLogin, resolvedFirstLogin);
        if (!requiresUpdate)
            return user;

        return userProvider.update(user.id(), username, resolvedFirstLogin, user.debugUser(), user.debugEnabled(), user.languageId())
                .orElseThrow(() -> new UserException("Failed to update user with ID: " + user.id()));
    }

    public boolean isOnline(int userId) {
        if (userId < 1) return false;
        return sessionProvider.findByUserId(userId).isPresent();
    }

    public @Nullable UserLogin getLastLogin(int userId) {
        return loginProvider.findByUserId(userId).stream()
                .max(Comparator.comparing(UserLogin::loginTime))
                .orElse(null);
    }
}
