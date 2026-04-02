package de.murmelmeister.murmelapi.maintenance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record MaintenanceImpl(
        int id,
        @Nullable String title,
        @Nullable String reason,
        @NotNull MaintenanceType status,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull LocalDateTime createdAt,
        int createdBy,
        @Nullable LocalDateTime changedAt,
        @Nullable Integer changedBy
) implements Maintenance {

    public MaintenanceImpl {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (title != null && title.length() > 64)
            throw new IllegalArgumentException("title cannot be longer than 64 characters");
        if (reason != null && reason.length() > 255)
            throw new IllegalArgumentException("reason cannot be longer than 255 characters");
        if (startAt.isAfter(endAt))
            throw new IllegalArgumentException("startAt must be before endAt");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public @NotNull Maintenance.Builder builder() {
        return new Builder(this);
    }

    public @NotNull Maintenance with(@NotNull Consumer<Maintenance.Builder> consumer) {
        Maintenance.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements Maintenance.Builder {
        private final int id;
        private final LocalDateTime createdAt;
        private final int createdBy;

        private String title;
        private String reason;
        private MaintenanceType status;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private LocalDateTime changedAt;
        private Integer changedBy;

        public Builder(@NotNull Maintenance maintenance) {
            this.id = maintenance.id();
            this.createdAt = maintenance.createdAt();
            this.createdBy = maintenance.createdBy();
            this.title = maintenance.title();
            this.reason = maintenance.reason();
            this.status = maintenance.status();
            this.startAt = maintenance.startAt();
            this.endAt = maintenance.endAt();
            this.changedAt = maintenance.changedAt();
            this.changedBy = maintenance.changedBy();
        }

        public @NotNull Maintenance.Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        public @NotNull Maintenance.Builder reason(@Nullable String reason) {
            this.reason = reason;
            return this;
        }

        public @NotNull Maintenance.Builder status(@NotNull MaintenanceType status) {
            this.status = status;
            return this;
        }

        public @NotNull Maintenance.Builder startAt(@NotNull LocalDateTime startAt) {
            this.startAt = startAt;
            return this;
        }

        public @NotNull Maintenance.Builder endAt(@NotNull LocalDateTime endAt) {
            this.endAt = endAt;
            return this;
        }

        public @NotNull Maintenance.Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull Maintenance.Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull Maintenance build() {
            return new MaintenanceImpl(id, title, reason, status, startAt, endAt, createdAt, createdBy, changedAt, changedBy);
        }
    }
}
