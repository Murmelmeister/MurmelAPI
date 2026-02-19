package de.murmelmeister.murmelapi.user.maintenance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

public record UserMaintenance(
        int id,
        int userId,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @Nullable String reason,
        @NotNull LocalDateTime createdAt,
        int createdBy,
        @Nullable LocalDateTime changedAt,
        @Nullable Integer changedBy
) {
    public UserMaintenance {
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (reason != null && reason.length() > 255)
            throw new IllegalArgumentException("reason cannot be longer than 255 characters");
    }

    public static @NotNull Builder builder(@NotNull UserMaintenance userMaintenance) {
        return new Builder(userMaintenance);
    }

    public static class Builder {
        private final int id;
        private final int userId;
        private final LocalDateTime createdAt;
        private final int createdBy;

        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private String reason;
        private LocalDateTime changedAt;
        private Integer changedBy;

        public Builder(@NotNull UserMaintenance userMaintenance) {
            this.id = userMaintenance.id();
            this.userId = userMaintenance.userId();
            this.startAt = userMaintenance.startAt();
            this.endAt = userMaintenance.endAt();
            this.reason = userMaintenance.reason();
            this.createdAt = userMaintenance.createdAt();
            this.createdBy = userMaintenance.createdBy();
            this.changedAt = userMaintenance.changedAt();
            this.changedBy = userMaintenance.changedBy();
        }

        public Builder startAt(@NotNull LocalDateTime startAt) {
            this.startAt = startAt;
            return this;
        }

        public Builder endAt(@NotNull LocalDateTime endAt) {
            this.endAt = endAt;
            return this;
        }

        public Builder reason(@Nullable String reason) {
            this.reason = reason;
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

        public @NotNull UserMaintenance build() {
            return new UserMaintenance(id, userId, startAt, endAt, reason, createdAt, createdBy, changedAt, changedBy);
        }
    }
}
