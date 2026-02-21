package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record PunishmentUser(
        @NotNull UUID userId,
        int typeId,
        @NotNull UUID logId
) {
    public PunishmentUser {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(logId, "logId cannot be null");
    }

    public static @NotNull Builder builder(@NotNull PunishmentUser currentUser) {
        return new Builder(currentUser);
    }

    public static class Builder {
        private final UUID userId;
        private final int typeId;

        private UUID logId;

        private Builder(@NotNull PunishmentUser currentUser) {
            this.userId = currentUser.userId();
            this.typeId = currentUser.typeId();
            this.logId = currentUser.logId();
        }

        public Builder logId(@NotNull UUID logId) {
            this.logId = logId;
            return this;
        }

        public @NotNull PunishmentUser build() {
            return new PunishmentUser(userId, typeId, logId);
        }
    }
}
