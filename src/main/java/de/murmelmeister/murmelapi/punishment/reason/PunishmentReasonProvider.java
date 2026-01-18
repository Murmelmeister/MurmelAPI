package de.murmelmeister.murmelapi.punishment.reason;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PunishmentReasonProvider {
    void refreshCache();

    @Nullable PunishmentReason getReason(int id);

    @Nullable List<PunishmentReason> getReasonsByType(int typeId);

    @NotNull List<PunishmentReason> getAllReasons();

    @Nullable PunishmentReason create(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs, boolean autoFlagIp, boolean autoPunish, int createdBy);

    int delete(int id);

    @Nullable PunishmentReason update(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs, boolean autoFlagIp, boolean autoPunish, int changedBy);
}
