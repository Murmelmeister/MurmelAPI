package de.murmelmeister.murmelapi.participant.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public record ParticipantPermission(
        int id,
        int participantId,
        @NotNull String permission,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public ParticipantPermission {
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public static @NotNull Builder builder(@NotNull ParticipantPermission participantPermission) {
        return new Builder(participantPermission);
    }

    public static class Builder {
        private final int id;
        private final int participantId;
        private final String permission;
        private final LocalDateTime createdAt;
        private final int createdBy;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        public Builder(@NotNull ParticipantPermission participantPermission) {
            this.id = participantPermission.id();
            this.participantId = participantPermission.participantId();
            this.permission = participantPermission.permission();
            this.createdAt = participantPermission.createdAt();
            this.createdBy = participantPermission.createdBy();
            this.expiresAt = participantPermission.expiresAt();
            this.changedBy = participantPermission.changedBy();
            this.changedAt = participantPermission.changedAt();
        }

        public Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull ParticipantPermission build() {
            return new ParticipantPermission(id, participantId, permission, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
