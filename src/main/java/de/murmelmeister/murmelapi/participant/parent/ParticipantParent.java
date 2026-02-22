package de.murmelmeister.murmelapi.participant.parent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public record ParticipantParent(
        int id,
        int participantId,
        int parentId,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public ParticipantParent {
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

    public static @NotNull Builder builder(@NotNull ParticipantParent participantParent) {
        return new Builder(participantParent);
    }

    public static class Builder {
        private final int id;
        private final int participantId;
        private final int parentId;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        public Builder(@NotNull ParticipantParent participantParent) {
            this.id = participantParent.id();
            this.participantId = participantParent.participantId();
            this.parentId = participantParent.parentId();
            this.expiresAt = participantParent.expiresAt();
            this.createdBy = participantParent.createdBy();
            this.createdAt = participantParent.createdAt();
            this.changedBy = participantParent.changedBy();
            this.changedAt = participantParent.changedAt();
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

        public @NotNull ParticipantParent build() {
            return new ParticipantParent(id, participantId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
