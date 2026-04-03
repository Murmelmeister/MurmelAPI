package de.murmelmeister.murmelapi.punishment.reason;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface PunishmentReason {
    int id();

    int typeId();

    @NotNull String reasonText();

    @Nullable Long durationSecs();

    boolean autoFlagIp();

    int createdBy();

    @NotNull LocalDateTime createdAt();

    @Nullable Integer changedBy();

    @Nullable LocalDateTime changedAt();

    boolean isPermanent();

    @NotNull Builder builder();

    @NotNull PunishmentReason with(@NotNull Consumer<Builder> consumer);

    static @NotNull PunishmentReason of(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs, boolean autoFlagIp, int createdBy, @NotNull LocalDateTime createdAt) {
        return new PunishmentReasonImpl(id, typeId, reasonText, durationSecs, autoFlagIp, createdBy, createdAt, null, null);
    }

    interface Builder {
        @NotNull Builder typeId(int typeId);

        @NotNull Builder reasonText(@NotNull String reasonText);

        @NotNull Builder durationSecs(@Nullable Long durationSecs);

        @NotNull Builder autoFlagIp(boolean autoFlagIp);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull PunishmentReason build();
    }
}
