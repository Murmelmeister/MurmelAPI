package de.murmelmeister.murmelapi.punishment.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

record PunishmentIpAddressImpl(
        @NotNull InetAddress inetAddress,
        int typeId,
        @NotNull UUID auditId,
        @Nullable LocalDateTime expiresAt
) implements PunishmentIpAddress {

    public PunishmentIpAddressImpl {
        Objects.requireNonNull(inetAddress, "inetAddress must not be null");
        Objects.requireNonNull(auditId, "auditId must not be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public @NotNull PunishmentIpAddress.Builder builder() {
        return new Builder(this);
    }

    public @NotNull PunishmentIpAddress with(@NotNull Consumer<PunishmentIpAddress.Builder> consumer) {
        PunishmentIpAddress.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements PunishmentIpAddress.Builder {
        private final InetAddress inetAddress;
        private final int typeId;

        private UUID auditId;
        private LocalDateTime expiresAt;

        private Builder(@NotNull PunishmentIpAddress currentIp) {
            this.inetAddress = currentIp.inetAddress();
            this.typeId = currentIp.typeId();
            this.auditId = currentIp.auditId();
            this.expiresAt = currentIp.expiresAt();
        }

        public @NotNull PunishmentIpAddress.Builder auditId(@NotNull UUID auditId) {
            this.auditId = auditId;
            return this;
        }

        public @NotNull PunishmentIpAddress.Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public @NotNull PunishmentIpAddress build() {
            return new PunishmentIpAddressImpl(inetAddress, typeId, auditId, expiresAt);
        }
    }
}
