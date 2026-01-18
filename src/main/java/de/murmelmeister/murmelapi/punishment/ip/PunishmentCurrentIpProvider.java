package de.murmelmeister.murmelapi.punishment.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface PunishmentCurrentIpProvider {
    void refreshCache();

    @NotNull
    @Unmodifiable
    List<InetAddress> getAllPunishedIps(int typeId);

    @Nullable PunishmentCurrentIp getPunishedIp(@NotNull InetAddress inetAddress, int typeId);

    @Nullable PunishmentCurrentIp create(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId);

    int delete(@NotNull InetAddress inetAddress, int typeId);

    @Nullable PunishmentCurrentIp update(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId);
}
