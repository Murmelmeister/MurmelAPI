package de.murmelmeister.murmelapi.user.session;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserSession(UUID id, int userId, LocalDateTime loginTime, String ipAddress, String clientBrand,
                          int protocolVersion) {
}

