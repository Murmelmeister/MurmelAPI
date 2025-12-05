package de.murmelmeister.murmelapi.user.session;

import java.util.List;
import java.util.UUID;

public interface UserSessionProvider {
    void refreshCache();

    UserSession findById(UUID sessionId);

    UserSession findByUserId(int userId);

    List<UserSession> findAll();

    UserSession create(int userId, String ipAddress, String clientBrand, int protocolVersion);

    int delete(UUID sessionId);
}
