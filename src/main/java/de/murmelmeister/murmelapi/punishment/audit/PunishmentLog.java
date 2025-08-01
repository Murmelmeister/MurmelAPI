package de.murmelmeister.murmelapi.punishment.audit;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a log entry for a punishment action.
 * This class is used to track the history of punishments applied to users.
 *
 * @param reasonId       ID of the reason, null if reason id deleted
 * @param reasonDuration Duration in seconds, null for permanent punishments
 */
public record PunishmentLog(UUID id, Action action,
                            Integer userId, String ipAddress, Integer reasonId, int reasonTypeId, String reasonText,
                            Long reasonDuration, boolean reasonAutoFlagIp, boolean reasonAutoPunish, int createdBy,
                            LocalDateTime createdAt) {

    public enum Action {
        CREATED, MODIFIED, REVOKED
    }

    public LocalDateTime expiresAt() {
        if (reasonDuration == null)
            return null; // Permanent punishments have no expiration
        return createdAt.plusSeconds(reasonDuration);
    }

    public boolean isExpired() {
        LocalDateTime expiresAt = expiresAt();
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return reasonDuration == null;
    }
}
