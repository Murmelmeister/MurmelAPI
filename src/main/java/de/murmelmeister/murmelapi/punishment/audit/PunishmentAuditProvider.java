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

    @NotNull Optional<PunishmentAudit> findAudit(@NotNull UUID id);

    @NotNull
    @Unmodifiable
    List<PunishmentAudit> findAudits(@NotNull UUID mojangId);

    @NotNull
    @Unmodifiable
    List<PunishmentAudit> findAudits(@NotNull InetAddress inetAddress);

    @NotNull Optional<PunishmentAudit> create(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int executorId);

    @NotNull Optional<PunishmentAudit> modify(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int executorId);

    @NotNull Optional<PunishmentAudit> revoke(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentAudit audit, int executorId);
}
