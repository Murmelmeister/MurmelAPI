package de.murmelmeister.murmelapi.maintenance.whitelist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public record MaintenanceWhitelist(
        int id,
        int maintenanceId,
        int userId,
        @Nullable LocalDateTime startAt,
        @Nullable LocalDateTime endAt,
        @Nullable String note,
        @NotNull LocalDateTime createdAt,
        int createdBy,
        @Nullable LocalDateTime changedAt,
        @Nullable Integer changedBy
) {
    public MaintenanceWhitelist {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (note != null && note.length() > 255)
            throw new IllegalArgumentException("note cannot be longer than 255 characters");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public static @NotNull Builder builder(@NotNull MaintenanceWhitelist maintenanceWhitelist) {
        return new Builder(maintenanceWhitelist);
    }

    public static class Builder {
        private final int id;
        private final int maintenanceId;
        private final int userId;
        private final LocalDateTime createdAt;
        private final int createdBy;

        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private String note;
        private LocalDateTime changedAt;
        private Integer changedBy;

        public Builder(@NotNull MaintenanceWhitelist maintenanceWhitelist) {
            this.id = maintenanceWhitelist.id();
            this.maintenanceId = maintenanceWhitelist.maintenanceId();
            this.userId = maintenanceWhitelist.userId();
            this.createdAt = maintenanceWhitelist.createdAt();
            this.createdBy = maintenanceWhitelist.createdBy();
            this.startAt = maintenanceWhitelist.startAt();
            this.endAt = maintenanceWhitelist.endAt();
            this.note = maintenanceWhitelist.note();
            this.changedAt = maintenanceWhitelist.changedAt();
            this.changedBy = maintenanceWhitelist.changedBy();
        }

        public Builder startAt(@Nullable LocalDateTime startAt) {
            this.startAt = startAt;
            return this;
        }

        public Builder endAt(@Nullable LocalDateTime endAt) {
            this.endAt = endAt;
            return this;
        }

        public Builder note(@Nullable String note) {
            this.note = note;
            return this;
        }

        public Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull MaintenanceWhitelist build() {
            return new MaintenanceWhitelist(id, maintenanceId, userId, startAt, endAt, note, createdAt, createdBy, changedAt, changedBy);
        }
    }
}
