package de.murmelmeister.murmelapi.punishment.reason;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public interface PunishmentReasonProvider {
    void refreshCache();

    @NotNull Optional<PunishmentReason> findReason(int id);

    @NotNull
    @Unmodifiable
    List<PunishmentReason> findReasons(int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentReason> findAll();

    @NotNull Optional<PunishmentReason> upsert(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs,
                                               boolean autoFlagIp, boolean autoPunish, int executorId);

    @NotNull Optional<PunishmentReason> upsert(@NotNull PunishmentReason reason, int executorId);

    int delete(int id);
}
