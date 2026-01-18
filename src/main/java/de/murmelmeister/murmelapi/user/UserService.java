package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.exceptions.user.UserException;
import de.murmelmeister.murmelapi.exceptions.user.UserPlayTimeException;
import de.murmelmeister.murmelapi.exceptions.user.UserSessionException;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTime;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProvider;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

public record UserService(
        @NotNull UserProvider userProvider,
        @NotNull UserPlayTimeProvider playTimeProvider,
        @NotNull UserLoginProvider loginProvider,
        @NotNull UserSessionProvider sessionProvider
) {
    public UserService {
        Objects.requireNonNull(userProvider, "userProvider must not be null");
        Objects.requireNonNull(playTimeProvider, "playTimeProvider must not be null");
        Objects.requireNonNull(loginProvider, "loginProvider must not be null");
        Objects.requireNonNull(sessionProvider, "sessionProvider must not be null");
    }

    public void startSession(int userId, @NotNull InetAddress inetAddress, @Nullable String clientBrand, int protocolVersion) {
        if (userId < 1)
            throw new IllegalArgumentException("Invalid parameters for session handling");

        UserSession session = sessionProvider.findByUserId(userId);
        if (session != null) {
            loginProvider.create(session);
            if (sessionProvider.delete(session.id()) < 1)
                throw new UserSessionException("Failed to delete existing session for user ID: " + userId);
        }
        sessionProvider.create(userId, inetAddress, clientBrand, protocolVersion);
    }

    public void closeSession(int userId) {
        if (userId < 1)
            throw new IllegalArgumentException("User ID must be greater than 0");

        UserSession session = sessionProvider.findByUserId(userId);
        if (session != null) {
            loginProvider.create(session);
            if (sessionProvider.delete(session.id()) < 1)
                throw new UserSessionException("Failed to delete session for user ID: " + userId);
        }
    }

    public void loginStreak(int userId) {
        // TODO: Check if login streak works correctly

        if (userId < 1)
            throw new IllegalArgumentException("User ID must be greater than 0");

        UserPlayTime playTime = playTimeProvider.findByUserId(userId);
        if (playTime == null)
            throw new UserPlayTimeException("Play time not found for user ID: " + userId);

        LocalDate today = LocalDate.now();
        LocalDate lastSeen = playTime.getLastSeenDate();
        if (lastSeen == null || lastSeen.plusDays(1).equals(today)) {
            playTime.setLoginCount(playTime.getLoginCount() + 1);
        } else if (!lastSeen.equals(today)) {
            playTime.setLoginCount(1);
        }

        playTime.setLastSeenDate(today);
        if (playTimeProvider.update(playTime) == null)
            throw new UserPlayTimeException("Failed to update play time for user ID: " + userId);
    }

    public void checkLoginStreakWhileOnline(int userId, @NotNull UserPlayTime playTime) {
        if (userId < 1) return;

        LocalDate today = LocalDate.now();
        LocalDate lastSeen = playTime.getLastSeenDate();
        if (lastSeen == null || lastSeen.isBefore(today)) {
            if (lastSeen != null && lastSeen.plusDays(1).equals(today))
                playTime.setLoginCount(playTime.getLoginCount() + 1);
            else playTime.setLoginCount(1);
            playTime.setLastSeenDate(today);
            if (playTimeProvider.update(playTime) == null)
                throw new UserPlayTimeException("Failed to update play time for user ID: " + userId);
        }
    }

    public @NotNull User join(@NotNull UUID uuid, @NotNull String username) {
        User user = userProvider.findByMojangId(uuid);
        if (user == null) {
            user = userProvider.create(uuid, username);
            if (user == null)
                throw new UserException("Failed to create user with UUID: " + uuid);
            userProvider.update(user.id(), username, LocalDateTime.now(), user.debugUser(), user.debugEnabled(), user.languageId());
        }

        UserPlayTime playTime = playTimeProvider.findByUserId(user.id());
        if (playTime == null) {
            playTime = playTimeProvider.create(user.id());
            if (playTime == null)
                throw new UserPlayTimeException("Failed to create play time for user with ID: " + user.id());
        }

        String currentUsername = user.username();
        if (!currentUsername.equals(username))
            userProvider.update(user.id(), username, user.firstLogin(), user.debugUser(), user.debugEnabled(), user.languageId());

        LocalDateTime firstJoin = user.firstLogin();
        if (firstJoin == null)
            userProvider.update(user.id(), user.username(), LocalDateTime.now(), user.debugUser(), user.debugEnabled(), user.languageId());

        if (playTime.getLastSeenDate() == null) {
            UserLogin userLogin = getLastLogin(user.id());
            LocalDateTime lastLogin = userLogin == null ? null : userLogin.loginTime();
            LocalDate lastSeen = lastLogin != null ? lastLogin.toLocalDate() : LocalDate.now();
            playTime.setLastSeenDate(lastSeen);
            if (!playTimeProvider.updateOnlyCache(playTime))
                throw new UserPlayTimeException("Failed to update last seen date for user with ID: " + user.id());
        }

        // TODO: Add refresh users
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
}
