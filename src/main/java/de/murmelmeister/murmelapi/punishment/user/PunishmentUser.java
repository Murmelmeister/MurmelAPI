package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public interface PunishmentUser {
    @NotNull UUID mojangId();

    int typeId();

    @NotNull UUID auditId();

    @Nullable LocalDateTime expiresAt();

    boolean isExpired();

    boolean isPermanent();

    @NotNull Builder builder();

    @NotNull PunishmentUser with(@NotNull Consumer<Builder> consumer);

    static @NotNull PunishmentUser of(@NotNull UUID mojangId, int typeId, @NotNull UUID auditId, @Nullable LocalDateTime expiresAt) {
        return new PunishmentUserImpl(mojangId, typeId, auditId, expiresAt);
    }

    interface Builder {
        @NotNull Builder auditId(@NotNull UUID auditId);

        @NotNull Builder expiresAt(@Nullable LocalDateTime expiresAt);

        @NotNull PunishmentUser build();
    }
}
