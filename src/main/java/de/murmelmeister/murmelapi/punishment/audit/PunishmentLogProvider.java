package de.murmelmeister.murmelapi.punishment.audit;

import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface PunishmentLogProvider {
    void refreshCache();

    @Nullable PunishmentLog findLog(@Nullable UUID logId);

    @NotNull
    @Unmodifiable
    List<PunishmentLog> findLogs(@NotNull UUID userId);

    @NotNull
    @Unmodifiable
    List<PunishmentLog> findLogs(@NotNull InetAddress inetAddress);

    @NotNull
    @Unmodifiable
    List<PunishmentLog> findLogs();

    @Nullable PunishmentLog create(@Nullable UUID userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy);

    @Nullable PunishmentLog modify(@Nullable UUID userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy);

    @Nullable PunishmentLog revoke(@Nullable UUID userId, @Nullable InetAddress inetAddress, @NotNull PunishmentLog log, int createdBy);
}
