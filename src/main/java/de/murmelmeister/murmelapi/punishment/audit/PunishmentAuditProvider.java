package de.murmelmeister.murmelapi.punishment.audit;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
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

    @ApiStatus.Internal
    static @NotNull PunishmentAuditProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new PunishmentAuditProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
