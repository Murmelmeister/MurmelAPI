package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

record PunishmentUserImpl(
        @NotNull UUID mojangId,
        int typeId,
        @NotNull UUID auditId,
        @Nullable LocalDateTime expiresAt
) implements PunishmentUser {

    public PunishmentUserImpl {
        Objects.requireNonNull(mojangId, "mojangId cannot be null");
        Objects.requireNonNull(auditId, "auditId cannot be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public @NotNull PunishmentUser.Builder builder() {
        return new Builder(this);
    }

    public @NotNull PunishmentUser with(@NotNull Consumer<PunishmentUser.Builder> consumer) {
        PunishmentUser.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements PunishmentUser.Builder {
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

        public @NotNull PunishmentUser.Builder auditId(@NotNull UUID auditId) {
            this.auditId = auditId;
            return this;
        }

        public @NotNull PunishmentUser.Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public @NotNull PunishmentUser build() {
            return new PunishmentUserImpl(userId, typeId, auditId, expiresAt);
        }
    }
}
