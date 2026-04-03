package de.murmelmeister.murmelapi.punishment.reason;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record PunishmentReasonImpl(
        int id,
        int typeId,
        @NotNull String reasonText,
        @Nullable Long durationSecs,
        boolean autoFlagIp,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) implements PunishmentReason {

    public PunishmentReasonImpl {
        Objects.requireNonNull(reasonText, "reasonText must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public boolean isPermanent() {
        return durationSecs == null;
    }

    public @NotNull PunishmentReason.Builder builder() {
        return new Builder(this);
    }

    public @NotNull PunishmentReason with(@NotNull Consumer<PunishmentReason.Builder> consumer) {
        PunishmentReason.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements PunishmentReason.Builder {
        private final int id;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private int typeId;
        private String reasonText;
        private Long durationSecs;
        private boolean autoFlagIp;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull PunishmentReason punishmentReason) {
            this.id = punishmentReason.id();
            this.typeId = punishmentReason.typeId();
            this.reasonText = punishmentReason.reasonText();
            this.durationSecs = punishmentReason.durationSecs();
            this.autoFlagIp = punishmentReason.autoFlagIp();
            this.createdBy = punishmentReason.createdBy();
            this.createdAt = punishmentReason.createdAt();
            this.changedBy = punishmentReason.changedBy();
            this.changedAt = punishmentReason.changedAt();
        }

        public @NotNull PunishmentReason.Builder typeId(int typeId) {
            this.typeId = typeId;
            return this;
        }

        public @NotNull PunishmentReason.Builder reasonText(@NotNull String reasonText) {
            this.reasonText = reasonText;
            return this;
        }

        public @NotNull PunishmentReason.Builder durationSecs(@Nullable Long durationSecs) {
            this.durationSecs = durationSecs;
            return this;
        }

        public @NotNull PunishmentReason.Builder autoFlagIp(boolean autoFlagIp) {
            this.autoFlagIp = autoFlagIp;
            return this;
        }

        public @NotNull PunishmentReason.Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull PunishmentReason.Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull PunishmentReason build() {
            return new PunishmentReasonImpl(id, typeId, reasonText, durationSecs, autoFlagIp, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
