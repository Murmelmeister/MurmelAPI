package de.murmelmeister.murmelapi.punishment.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface PunishmentIpAddressProvider {
    void refreshCache();

    @Nullable PunishmentIpAddress findPunishedIpAddress(@NotNull InetAddress inetAddress, int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentIpAddress> findPunishedIpAddresses(int typeId);

    @Nullable PunishmentIpAddress create(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId);

    int delete(@NotNull InetAddress inetAddress, int typeId);

    @Nullable PunishmentIpAddress update(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId);
}
