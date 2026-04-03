package de.murmelmeister.murmelapi.punishment.ip;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PunishmentIpAddressProvider {
    void refreshCache();

    @NotNull Optional<PunishmentIpAddress> findPunishedIpAddress(@NotNull InetAddress inetAddress, int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentIpAddress> findPunishedIpAddresses(int typeId);

    @NotNull Optional<PunishmentIpAddress> upsert(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID auditId, @Nullable Long durationSecs);

    int delete(@NotNull InetAddress inetAddress, int typeId);

    int loadExpired();

    @ApiStatus.Internal
    static @NotNull PunishmentIpAddressProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new PunishmentIpAddressProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
