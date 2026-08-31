package de.murmelmeister.murmelapi.maintenance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface Maintenance {
    int id();

    @Nullable String title();

    @Nullable String reason();

    @NotNull MaintenanceType status();

    @NotNull LocalDateTime startAt();

    @NotNull LocalDateTime endAt();

    @NotNull LocalDateTime createdAt();

    int createdBy();

    @Nullable LocalDateTime changedAt();

    @Nullable Integer changedBy();

    @NotNull Builder builder();

    @NotNull Maintenance with(@NotNull Consumer<Builder> consumer);

    default boolean isEnded() {
        return endAt().isBefore(LocalDateTime.now());
    }

    default boolean isStarted() {
        return startAt().isBefore(LocalDateTime.now());
    }

    static @NotNull Maintenance of(int id, @Nullable String title, @Nullable String reason, @NotNull MaintenanceType status, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @NotNull LocalDateTime createdAt, int createdBy) {
        return new MaintenanceImpl(id, title, reason, status, startAt, endAt, createdAt, createdBy, null, null);
    }

    interface Builder {
        @NotNull Builder title(@Nullable String title);

        @NotNull Builder reason(@Nullable String reason);

        @NotNull Builder status(@NotNull MaintenanceType status);

        @NotNull Builder startAt(@NotNull LocalDateTime startAt);

        @NotNull Builder endAt(@NotNull LocalDateTime endAt);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull Maintenance build();
    }
}
