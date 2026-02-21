package de.murmelmeister.murmelapi.punishment.reason;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface PunishmentReasonProvider {
    void refreshCache();

    @Nullable PunishmentReason getReason(int id);

    @NotNull
    @Unmodifiable
    List<PunishmentReason> getReasonsByType(int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentReason> getAllReasons();

    @Nullable PunishmentReason create(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs, boolean autoFlagIp, boolean autoPunish, int createdBy);

    int delete(int id);

    @Nullable PunishmentReason update(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs, boolean autoFlagIp, boolean autoPunish, int changedBy);

    @Nullable PunishmentReason upsert(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs,
                                      boolean autoFlagIp, boolean autoPunish, int executorId);

    @Nullable PunishmentReason upsert(@NotNull PunishmentReason reason, int executorId);
}
