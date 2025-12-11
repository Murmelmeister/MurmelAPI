package de.murmelmeister.murmelapi.user.login;

import de.murmelmeister.murmelapi.user.session.UserSession;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserLoginProvider {
    void refreshCache();

    UserLogin findById(UUID id);

    List<UserLogin> findByUserId(int userId);

    List<UserLogin> findByIpAddress(InetAddress inetAddress);

    List<UserLogin> findAll();

    UserLogin create(UUID sessionId, int userId, LocalDateTime loginTime, InetAddress inetAddress, String clientBrand, int protocolVersion);

    UserLogin create(UserSession session);

    int delete(UUID id);
}
