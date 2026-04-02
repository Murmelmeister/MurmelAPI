package de.murmelmeister.murmelapi.maintenance.whitelist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface MaintenanceWhitelist {
    int id();

    int maintenanceId();

    int userId();

    @Nullable LocalDateTime startAt();

    @Nullable LocalDateTime endAt();

    @Nullable String note();

    @NotNull LocalDateTime createdAt();

    int createdBy();

    @Nullable LocalDateTime changedAt();

    @Nullable Integer changedBy();

    @NotNull Builder builder();

    @NotNull MaintenanceWhitelist with(@NotNull Consumer<Builder> consumer);

    static @NotNull MaintenanceWhitelist of(int id, int maintenanceId, int userId, @Nullable LocalDateTime startAt, @Nullable LocalDateTime endAt, @Nullable String note, @NotNull LocalDateTime createdAt, int createdBy) {
        return new MaintenanceWhitelistImpl(id, maintenanceId, userId, startAt, endAt, note, createdAt, createdBy, null, null);
    }

    interface Builder {
        @NotNull Builder startAt(@Nullable LocalDateTime startAt);

        @NotNull Builder endAt(@Nullable LocalDateTime endAt);

        @NotNull Builder note(@Nullable String note);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull MaintenanceWhitelist build();
    }
}
