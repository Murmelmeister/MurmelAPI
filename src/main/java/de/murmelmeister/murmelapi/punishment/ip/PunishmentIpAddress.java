package de.murmelmeister.murmelapi.punishment.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public interface PunishmentIpAddress {
    @NotNull InetAddress inetAddress();

    int typeId();

    @NotNull UUID auditId();

    @Nullable LocalDateTime expiresAt();

    boolean isExpired();

    boolean isPermanent();

    @NotNull Builder builder();

    @NotNull PunishmentIpAddress with(@NotNull Consumer<Builder> consumer);

    static @NotNull PunishmentIpAddress of(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID auditId, @Nullable LocalDateTime expiresAt) {
        return new PunishmentIpAddressImpl(inetAddress, typeId, auditId, expiresAt);
    }

    interface Builder {
        @NotNull Builder auditId(@NotNull UUID auditId);

        @NotNull Builder expiresAt(@Nullable LocalDateTime expiresAt);

        @NotNull PunishmentIpAddress build();
    }
}
