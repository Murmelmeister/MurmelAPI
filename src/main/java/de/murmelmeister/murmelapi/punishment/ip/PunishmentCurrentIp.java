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

    public @NotNull PunishmentCurrentIp withUpdateLog(UUID logId) {
        return new PunishmentCurrentIp(inetAddress, typeId, logId != null ? logId : this.logId);
    }
}
