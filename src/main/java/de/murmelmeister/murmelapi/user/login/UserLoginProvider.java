package de.murmelmeister.murmelapi.user.login;

import de.murmelmeister.murmelapi.user.session.UserSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserLoginProvider {
    void closeCache();

    void refreshCache();

    UserLogin findById(UUID id);

    List<UserLogin> findByUserId(int userId);

    List<UserLogin> findByIpAddress(String ipAddress);

    List<UserLogin> findAllLogins();

    UserLogin create(UUID sessionId, int userId, LocalDateTime loginTime, String ipAddress, String clientVersion, String protocolVersion);

    UserLogin create(UserSession session);

    int delete(UUID id);

    UserLogin getLastLogin(int userId);

    LocalDateTime getLastLoginTime(int userId);
}
