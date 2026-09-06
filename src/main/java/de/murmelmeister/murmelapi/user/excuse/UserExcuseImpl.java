package de.murmelmeister.murmelapi.user.excuse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record UserExcuseImpl(
        int id,
        int userId,
        @NotNull LocalDate startDate,
        int extraDays,
        @Nullable String reason,
        @NotNull LocalDateTime createdAt,
        int createdBy,
        @Nullable LocalDateTime changedAt,
        @Nullable Integer changedBy
) implements UserExcuse {

    public UserExcuseImpl {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (extraDays < 0) throw new IllegalArgumentException("extraDays must be >= 0");
        if (reason != null && reason.length() > 255)
            throw new IllegalArgumentException("reason cannot be longer than 255 characters");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public @NotNull UserExcuse.Builder builder() {
        return new Builder(this);
    }

    public @NotNull UserExcuse with(@NotNull Consumer<UserExcuse.Builder> consumer) {
        UserExcuse.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements UserExcuse.Builder {
        private final int id;
        private final int userId;
        private final LocalDateTime createdAt;
        private final int createdBy;

        private LocalDate startDate;
        private int extraDays;
        private String reason;
        private LocalDateTime changedAt;
        private Integer changedBy;

        public Builder(@NotNull UserExcuse userExcuse) {
            this.id = userExcuse.id();
            this.userId = userExcuse.userId();
            this.startDate = userExcuse.startDate();
            this.extraDays = userExcuse.extraDays();
            this.reason = userExcuse.reason();
            this.createdAt = userExcuse.createdAt();
            this.createdBy = userExcuse.createdBy();
            this.changedAt = userExcuse.changedAt();
            this.changedBy = userExcuse.changedBy();
        }

        public @NotNull UserExcuse.Builder startDate(@NotNull LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public @NotNull UserExcuse.Builder extraDays(int extraDays) {
            this.extraDays = extraDays;
            return this;
        }

        public @NotNull UserExcuse.Builder reason(@Nullable String reason) {
            this.reason = reason;
            return this;
        }

        public @NotNull UserExcuse.Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull UserExcuse.Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull UserExcuse build() {
            return new UserExcuseImpl(id, userId, startDate, extraDays, reason, createdAt, createdBy, changedAt, changedBy);
        }
    }
}
