package de.murmelmeister.murmelapi.user.session;

import java.util.List;
import java.util.UUID;

public interface UserSessionProvider {
    void closeCache();

    void refreshCache();

    UserSession findById(UUID sessionId);

    UserSession findByUserId(int userId);

    List<UserSession> findAll();

    List<UserSession> findUserSessions(int userId);

    UserSession create(int userId, String ipAddress, String clientVersion, String protocolVersion);

    int delete(UUID sessionId);

    boolean isOnline(int userId);
}
