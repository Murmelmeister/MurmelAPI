package de.murmelmeister.murmelapi.punishment.ip;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;

public record PunishmentCurrentIp(
        @NotNull InetAddress inetAddress,
        int typeId,
        @NotNull UUID logId
) {
    public PunishmentCurrentIp {
        Objects.requireNonNull(inetAddress, "inetAddress must not be null");
        Objects.requireNonNull(logId, "logId must not be null");
    }

    public static @NotNull Builder builder(@NotNull PunishmentCurrentIp currentIp) {
        return new Builder(currentIp);
    }

    public static class Builder {
        private final InetAddress inetAddress;
        private final int typeId;

        private UUID logId;

        private Builder(@NotNull PunishmentCurrentIp currentIp) {
            this.inetAddress = currentIp.inetAddress();
            this.typeId = currentIp.typeId();
            this.logId = currentIp.logId();
        }

        public Builder logId(@NotNull UUID logId) {
            this.logId = logId;
            return this;
        }

        public @NotNull PunishmentCurrentIp build() {
            return new PunishmentCurrentIp(inetAddress, typeId, logId);
        }
    }
}
