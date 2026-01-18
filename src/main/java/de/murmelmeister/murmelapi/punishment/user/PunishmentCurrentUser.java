package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record PunishmentCurrentUser(
        int userId,
        int typeId,
        @NotNull UUID logId
) {
    public PunishmentCurrentUser {
        Objects.requireNonNull(logId, "Log ID cannot be null");
    }

    public static @NotNull Builder builder(@NotNull PunishmentCurrentUser currentUser) {
        return new Builder(currentUser);
    }

    public static class Builder {
        private final int userId;
        private final int typeId;

        private UUID logId;

        private Builder(@NotNull PunishmentCurrentUser currentUser) {
            this.userId = currentUser.userId();
            this.typeId = currentUser.typeId();
            this.logId = currentUser.logId();
        }

        public Builder logId(@NotNull UUID logId) {
            this.logId = logId;
            return this;
        }

        public @NotNull PunishmentCurrentUser build() {
            return new PunishmentCurrentUser(userId, typeId, logId);
        }
    }
}
