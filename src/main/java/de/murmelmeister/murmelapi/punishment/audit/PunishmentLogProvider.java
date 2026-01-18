package de.murmelmeister.murmelapi.punishment.audit;

import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface PunishmentLogProvider {
    void refreshCache();

    @Nullable PunishmentLog getLog(@Nullable UUID logId);

    @Nullable List<PunishmentLog> getLogsByUserId(int userId);

    @Nullable List<PunishmentLog> getLogsByIpAddress(@NotNull InetAddress inetAddress);

    @NotNull List<PunishmentLog> getLogs();

    @Nullable PunishmentLog create(@Nullable Integer userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy);

    @Nullable PunishmentLog modify(@Nullable Integer userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy);

    @Nullable PunishmentLog revoke(@Nullable Integer userId, @Nullable InetAddress inetAddress, @NotNull PunishmentLog log, int createdBy);
}
