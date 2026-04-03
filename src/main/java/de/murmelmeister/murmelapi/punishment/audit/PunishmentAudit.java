package de.murmelmeister.murmelapi.punishment.audit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

public interface PunishmentAudit {
    @NotNull UUID id();

    @NotNull Action action();

    @Nullable UUID mojangId();

    @Nullable InetAddress inetAddress();

    @Nullable Integer reasonId();

    @Nullable Integer reasonTypeId();

    @NotNull String reasonText();

    @Nullable Long reasonDuration();

    boolean reasonAutoFlagIp();

    @Nullable Integer createdBy();

    @NotNull LocalDateTime createdAt();

    enum Action {
        CREATED, MODIFIED, REVOKED
    }

    @Nullable LocalDateTime expiresAt();

    boolean isExpired();

    boolean isPermanent();

    static @NotNull PunishmentAudit of(@NotNull UUID id, @NotNull Action action, @Nullable UUID mojangId, @Nullable InetAddress inetAddress,
                                  @Nullable Integer reasonId, @Nullable Integer reasonTypeId, @NotNull String reasonText,
                                  @Nullable Long reasonDuration, boolean reasonAutoFlagIp, @Nullable Integer createdBy,
                                  @NotNull LocalDateTime createdAt) {
        return new PunishmentAuditImpl(id, action, mojangId, inetAddress, reasonId, reasonTypeId, reasonText,
                reasonDuration, reasonAutoFlagIp, createdBy, createdAt);
    }
}
