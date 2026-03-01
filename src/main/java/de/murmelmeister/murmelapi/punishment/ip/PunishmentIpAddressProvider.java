package de.murmelmeister.murmelapi.punishment.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PunishmentIpAddressProvider {
    void refreshCache();

    @NotNull Optional<PunishmentIpAddress> findPunishedIpAddress(@NotNull InetAddress inetAddress, int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentIpAddress> findPunishedIpAddresses(int typeId);

    @NotNull Optional<PunishmentIpAddress> upsert(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId, @Nullable Long durationSecs);

    int delete(@NotNull InetAddress inetAddress, int typeId);

    int loadExpired();
}
