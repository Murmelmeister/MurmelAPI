package de.murmelmeister.murmelapi.punishment.reason;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * @param durationSecs Duration in seconds, null for permanent reasons
 */
public record PunishmentReason(
        int id,
        int typeId,
        @NotNull String reasonText,
        @Nullable Long durationSecs,
        boolean autoFlagIp,
        boolean autoPunish,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public PunishmentReason {
        Objects.requireNonNull(reasonText, "reasonText must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public boolean isPermanent() {
        return durationSecs == null;
    }

    public static @NotNull Builder builder(@NotNull PunishmentReason punishmentReason) {
        return new Builder(punishmentReason);
    }

    public static class Builder {
        private final int id;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private int typeId;
        private String reasonText;
        private Long durationSecs;
        private boolean autoFlagIp;
        private boolean autoPunish;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull PunishmentReason punishmentReason) {
            this.id = punishmentReason.id();
            this.typeId = punishmentReason.typeId();
            this.reasonText = punishmentReason.reasonText();
            this.durationSecs = punishmentReason.durationSecs();
            this.autoFlagIp = punishmentReason.autoFlagIp();
            this.autoPunish = punishmentReason.autoPunish();
            this.createdBy = punishmentReason.createdBy();
            this.createdAt = punishmentReason.createdAt();
            this.changedBy = punishmentReason.changedBy();
            this.changedAt = punishmentReason.changedAt();
        }

        public Builder typeId(int typeId) {
            this.typeId = typeId;
            return this;
        }

        public Builder reasonText(@NotNull String reasonText) {
            this.reasonText = reasonText;
            return this;
        }

        public Builder durationSecs(@Nullable Long durationSecs) {
            this.durationSecs = durationSecs;
            return this;
        }

        public Builder autoFlagIp(boolean autoFlagIp) {
            this.autoFlagIp = autoFlagIp;
            return this;
        }

        public Builder autoPunish(boolean autoPunish) {
            this.autoPunish = autoPunish;
            return this;
        }

        public Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull PunishmentReason build() {
            return new PunishmentReason(id, typeId, reasonText, durationSecs, autoFlagIp, autoPunish, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
