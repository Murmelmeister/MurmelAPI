package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record PunishmentUser(
        @NotNull UUID mojangId,
        int typeId,
        @NotNull UUID auditId,
        @Nullable LocalDateTime expiresAt
) {
    public PunishmentUser {
        Objects.requireNonNull(mojangId, "mojangId cannot be null");
        Objects.requireNonNull(auditId, "auditId cannot be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public static @NotNull Builder builder(@NotNull PunishmentUser currentUser) {
        return new Builder(currentUser);
    }

    public static class Builder {
        private final UUID userId;
        private final int typeId;

        private UUID auditId;
        private LocalDateTime expiresAt;

        private Builder(@NotNull PunishmentUser currentUser) {
            this.userId = currentUser.mojangId();
            this.typeId = currentUser.typeId();
            this.auditId = currentUser.auditId();
            this.expiresAt = currentUser.expiresAt();
        }

        public Builder auditId(@NotNull UUID auditId) {
            this.auditId = auditId;
            return this;
        }

        public Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public @NotNull PunishmentUser build() {
            return new PunishmentUser(userId, typeId, auditId, expiresAt);
        }
    }
}
