package de.murmelmeister.murmelapi.user.excuse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

public record UserExcuse(
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
    public UserExcuse {
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (reason != null && reason.length() > 255)
            throw new IllegalArgumentException("reason cannot be longer than 255 characters");
    }

    public static @NotNull Builder builder(@NotNull UserExcuse userExcuse) {
        return new Builder(userExcuse);
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

        public Builder(@NotNull UserExcuse userExcuse) {
            this.id = userExcuse.id();
            this.userId = userExcuse.userId();
            this.startAt = userExcuse.startAt();
            this.endAt = userExcuse.endAt();
            this.reason = userExcuse.reason();
            this.createdAt = userExcuse.createdAt();
            this.createdBy = userExcuse.createdBy();
            this.changedAt = userExcuse.changedAt();
            this.changedBy = userExcuse.changedBy();
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

        public @NotNull UserExcuse build() {
            return new UserExcuse(id, userId, startAt, endAt, reason, createdAt, createdBy, changedAt, changedBy);
        }
    }
}
