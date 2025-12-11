package de.murmelmeister.murmelapi.user.session;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface UserSessionProvider {
    void refreshCache();

    UserSession findById(UUID sessionId);

    UserSession findByUserId(int userId);

    List<UserSession> findAll();

    UserSession create(int userId, InetAddress inetAddress, String clientBrand, int protocolVersion);

    int delete(UUID sessionId);
}
