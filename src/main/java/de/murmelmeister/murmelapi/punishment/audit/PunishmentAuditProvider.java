package de.murmelmeister.murmelapi.punishment.audit;

import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PunishmentAuditProvider {
    void refreshCache();

    @NotNull Optional<PunishmentAudit> findLog(@NotNull UUID logId);

    @NotNull
    @Unmodifiable
    List<PunishmentAudit> findLogs(@NotNull UUID mojangId);

    @NotNull
    @Unmodifiable
    List<PunishmentAudit> findLogs(@NotNull InetAddress inetAddress);

    @NotNull Optional<PunishmentAudit> create(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy);

    @NotNull Optional<PunishmentAudit> modify(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy);

    @NotNull Optional<PunishmentAudit> revoke(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentAudit log, int createdBy);
}
