package de.murmelmeister.murmelapi.punishment.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record PunishmentIpAddress(
        @NotNull InetAddress inetAddress,
        int typeId,
        @NotNull UUID logId,
        @Nullable LocalDateTime expiresAt
) {
    public PunishmentIpAddress {
        Objects.requireNonNull(inetAddress, "inetAddress must not be null");
        Objects.requireNonNull(logId, "logId must not be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public static @NotNull Builder builder(@NotNull PunishmentIpAddress currentIp) {
        return new Builder(currentIp);
    }

    public static class Builder {
        private final InetAddress inetAddress;
        private final int typeId;

        private UUID logId;
        private LocalDateTime expiresAt;

        private Builder(@NotNull PunishmentIpAddress currentIp) {
            this.inetAddress = currentIp.inetAddress();
            this.typeId = currentIp.typeId();
            this.logId = currentIp.logId();
            this.expiresAt = currentIp.expiresAt();
        }

        public Builder logId(@NotNull UUID logId) {
            this.logId = logId;
            return this;
        }

        public Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public @NotNull PunishmentIpAddress build() {
            return new PunishmentIpAddress(inetAddress, typeId, logId, expiresAt);
        }
    }
}
