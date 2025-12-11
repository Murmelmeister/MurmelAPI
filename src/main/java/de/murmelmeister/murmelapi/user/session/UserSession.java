package de.murmelmeister.murmelapi.user.session;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserSession(UUID id, int userId, LocalDateTime loginTime, InetAddress inetAddress, String clientBrand,
                          int protocolVersion) {
}

