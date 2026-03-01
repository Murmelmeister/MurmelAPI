package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.exceptions.user.UserException;
import de.murmelmeister.murmelapi.exceptions.user.UserSessionException;
import de.murmelmeister.murmelapi.exceptions.user.UserStatsException;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAudit;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAuditProvider;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUserProvider;
import de.murmelmeister.murmelapi.user.excuse.UserExcuseProvider;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.murmelapi.user.stats.UserStats;
import de.murmelmeister.murmelapi.user.stats.UserStatsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public record UserService(
        @NotNull UserProvider userProvider,
        @NotNull UserStatsProvider statsProvider,
        @NotNull UserLoginProvider loginProvider,
        @NotNull UserSessionProvider sessionProvider,
        @NotNull UserExcuseProvider userExcuseProvider,
        @NotNull PunishmentUserProvider punishUserProvider,
        @NotNull PunishmentAuditProvider punishAuditProvider
) {
    public UserService {
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

        UserSession session = sessionProvider.findByUserId(userId);
        if (session != null) {
            loginProvider.create(session);
            if (sessionProvider.delete(session.id()) < 1)
                throw new UserSessionException("Failed to delete existing session for userId: " + userId);
        }
        sessionProvider.create(userId, inetAddress, clientBrand, protocolVersion);
    }

    public void closeSession(int userId) {
        if (userId < 1)
            throw new IllegalArgumentException("userId must be >= 1");

        UserSession session = sessionProvider.findByUserId(userId);
        if (session != null) {
            loginProvider.create(session);
            if (sessionProvider.delete(session.id()) < 1)
                throw new UserSessionException("Failed to delete session for userId: " + userId);
        }
    }

    public void loginStreak(int userId) {
        if (userId < 1)
            throw new IllegalArgumentException("userId must be >= 1");
        loginStreak(
                userId,
                ZoneId.systemDefault(),
                this::isExcusedOnDay,
                this::isPermanentlyBlocked
        );
    }

    public void checkLoginStreakWhileOnline(int userId, @NotNull UserStats stats) {
        Objects.requireNonNull(stats, "stats must not be null");
        if (userId < 1) throw new IllegalArgumentException("User ID must be >= 1");

        checkLoginStreakWhileOnline(
                userId,
                stats,
                ZoneId.systemDefault(),
                this::isExcusedOnDay,
                this::isPermanentlyBlocked
        );
    }

    public @NotNull User join(@NotNull UUID uuid, @NotNull String username) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        Objects.requireNonNull(username, "username must not be null");

        User user = userProvider.findByMojangId(uuid);
        if (user == null) {
            user = userProvider.create(uuid, username);
            if (user == null)
                throw new UserException("Failed to create user with UUID: " + uuid);
            userProvider.update(user.id(), username, LocalDateTime.now(), user.debugUser(), user.debugEnabled(), user.languageId());
        }

        UserStats stats = statsProvider.findByUserId(user.id());
        if (stats == null) {
            stats = statsProvider.create(user.id());
            if (stats == null)
                throw new UserStatsException("Failed to create stats for user with ID: " + user.id());
        }

        String currentUsername = user.username();
        if (!currentUsername.equals(username))
            userProvider.update(user.id(), username, user.firstLogin(), user.debugUser(), user.debugEnabled(), user.languageId());

        LocalDateTime firstJoin = user.firstLogin();
        if (firstJoin == null)
            userProvider.update(user.id(), user.username(), LocalDateTime.now(), user.debugUser(), user.debugEnabled(), user.languageId());

        if (stats.dailyStreakLastDay() == null) {
            UserLogin userLogin = getLastLogin(user.id());
            LocalDateTime lastLogin = userLogin == null ? null : userLogin.loginTime();
            LocalDate lastSeen = lastLogin != null ? lastLogin.toLocalDate() : LocalDate.now();

            if (statsProvider.update(user.id(), stats.playTime(), stats.dailyStreak(), lastSeen, stats.lastSeenAt()) == null)
                throw new UserStatsException("Failed to update stats for user with ID: " + user.id());
        }

        return user;
    }

    public boolean isOnline(int userId) {
        if (userId < 1) return false;
        return sessionProvider.findByUserId(userId) != null;
    }

    public @Nullable UserLogin getLastLogin(int userId) {
        return loginProvider.findByUserId(userId).stream()
                .max(Comparator.comparing(UserLogin::loginTime))
                .orElse(null);
    }

    /**
     * Daily streak update for "user was active today" (login or first tick after midnight while online).
     * <p>
     * Rules:
     * - 1 second of activity counts for the day.
     * - Gap days only break the streak if NOT all gap days are forgiven.
     * - Forgiven days are NOT added to the streak, they only prevent breaking it.
     * - Permanent user/IP bans should break the streak (reset), so no unnecessary state is kept.
     *
     * @param userId               user ID
     * @param zoneId               time zone used to determine "calendar day"
     * @param isForgivenDay        true if the user could not/was not allowed to join on that day
     *                             (maintenance/outage/excuse/temporary ban)
     * @param isPermanentlyBlocked true if the user is permanently banned (then reset)
     */
    public void loginStreak(int userId,
                            ZoneId zoneId,
                            BiPredicate<Integer, LocalDate> isForgivenDay,
                            Predicate<UUID> isPermanentlyBlocked) {

        if (userId < 1)
            throw new IllegalArgumentException("userId must be >= 1");
        Objects.requireNonNull(zoneId, "zoneId must not be null");
        Objects.requireNonNull(isForgivenDay, "isForgivenDay must not be null");
        Objects.requireNonNull(isPermanentlyBlocked, "isPermanentlyBlocked must not be null");

        User user = userProvider.findById(userId);
        if (user == null)
            throw new UserException("User not found for ID: " + userId);

        UserStats stats = statsProvider.findByUserId(userId);
        if (stats == null)
            throw new UserStatsException("Stats not found for user ID: " + userId);

        // Permanent block => breaks the streak (and keeps the DB state clean)
        if (isPermanentlyBlocked.test(user.mojangId())) {
            if (statsProvider.update(userId, stats.playTime(), 0, null, stats.lastSeenAt()) == null)
                throw new UserStatsException("Failed to update stats for user ID: " + userId);
            return;
        }

        LocalDate today = LocalDate.now(zoneId);
        LocalDate lastDay = stats.dailyStreakLastDay();
        int dailyStreak = stats.dailyStreak();

        // Never counted before => start with 1
        if (lastDay == null) {
            if (statsProvider.update(userId, stats.playTime(), 1, today, stats.lastSeenAt()) == null)
                throw new UserStatsException("Failed to update stats for user ID: " + userId);
            return;
        }

        // Day already counted => no-op
        if (lastDay.equals(today)) return;

        // Normal next day => +1
        if (lastDay.plusDays(1).equals(today)) {
            dailyStreak++;
            if (statsProvider.update(userId, stats.playTime(), dailyStreak, today, stats.lastSeenAt()) == null)
                throw new UserStatsException("Failed to update stats for user ID: " + userId);
            return;
        }

        // Gap (> 1 day): check whether ALL missing days are forgiven.
        boolean gapFullyForgiven = isGapFullyForgiven(userId, lastDay, today, isForgivenDay);

        if (gapFullyForgiven) {
            // Streak remains, but only "today" is counted (+1)
            dailyStreak++;
        } else {
            // Real interruption
            dailyStreak = 1;
        }

        if (statsProvider.update(userId, stats.playTime(), dailyStreak, today, stats.lastSeenAt()) == null)
            throw new UserStatsException("Failed to update stats for user ID: " + userId);
    }

    /**
     * Called e.g., by a scheduler (while the user is online) to handle midnight transitions.
     * This method is intentionally only a wrapper around loginStreak(...), so logic is maintained in one place.
     */
    public void checkLoginStreakWhileOnline(int userId,
                                            @NotNull UserStats stats,
                                            ZoneId zoneId,
                                            BiPredicate<Integer, LocalDate> isForgivenDay,
                                            Predicate<UUID> isPermanentlyBlocked) {
        Objects.requireNonNull(stats, "stats must not be null");
        Objects.requireNonNull(zoneId, "zoneId must not be null");
        Objects.requireNonNull(isForgivenDay, "isForgivenDay must not be null");
        Objects.requireNonNull(isPermanentlyBlocked, "isPermanentlyBlocked must not be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");

        // Only relevant condition: "User was active today and has not been counted yet"
        LocalDate today = LocalDate.now(zoneId);
        LocalDate lastDay = stats.dailyStreakLastDay();
        if (lastDay == null || lastDay.isBefore(today)) {
            loginStreak(userId, zoneId, isForgivenDay, isPermanentlyBlocked);
        }
    }

    private boolean isGapFullyForgiven(int userId,
                                       LocalDate lastDay,
                                       LocalDate today,
                                       BiPredicate<Integer, LocalDate> isForgivenDay) {
        // gap days are: (lastDay+1) .. (today-1)
        for (LocalDate d = lastDay.plusDays(1); d.isBefore(today); d = d.plusDays(1)) {
            if (!isForgivenDay.test(userId, d))
                return false;
        }
        return true;
    }

    private boolean isExcusedOnDay(int userId, @NotNull LocalDate day) {
        if (userId < 1) return false;
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime nextDayStart = day.plusDays(1).atStartOfDay();

        return userExcuseProvider.findByUserId(userId).stream()
                .anyMatch(excuse -> excuse.startAt().isBefore(nextDayStart) && !excuse.endAt().isBefore(dayStart));
    }

    private boolean isPermanentlyBlocked(UUID mojangId) {
        if (mojangId == null) return false;
        PunishmentUser punishUser = punishUserProvider.findPunishedUser(mojangId, PunishmentType.BAN.getId()).orElse(null);
        if (punishUser == null) return false;

        PunishmentAudit punishmentAudit = punishAuditProvider.findLog(punishUser.logId()).orElse(null);
        return punishmentAudit != null && punishmentAudit.isPermanent();
    }
}
