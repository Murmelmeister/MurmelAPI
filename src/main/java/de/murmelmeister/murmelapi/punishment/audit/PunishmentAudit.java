package de.murmelmeister.murmelapi.punishment.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * Represents a log entry for a punishment action.
 * This class is used to track the history of punishments applied to users.
 *
 * @param reasonId       ID of the reason, null if reason id deleted
 * @param reasonDuration Duration in seconds, null for permanent punishments
 */
public record PunishmentAudit(
        @NotNull UUID id,
        @NotNull Action action,
        @Nullable UUID mojangId,
        @Nullable InetAddress inetAddress,
        @Nullable Integer reasonId,
        @Nullable Integer reasonTypeId,
        @NotNull String reasonText,
        @Nullable Long reasonDuration,
        boolean reasonAutoFlagIp,
        boolean reasonAutoPunish,
        @Nullable Integer createdBy,
        @NotNull LocalDateTime createdAt
) {
    public PunishmentAudit {
        Objects.requireNonNull(id, "logId cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(reasonText, "reasonText cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        if (createdBy != null && createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
    }

    public enum Action {
        CREATED, MODIFIED, REVOKED
    }

    public @Nullable LocalDateTime expiresAt() {
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
