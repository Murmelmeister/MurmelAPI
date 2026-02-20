package de.murmelmeister.murmelapi.maintenance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.LocalDateTime;
import java.util.List;

public interface MaintenanceProvider {
    void refreshCache();

    @Nullable Maintenance findById(int id);

    @NotNull
    @Unmodifiable
    List<Maintenance> findAll();

    @Nullable Maintenance create(@Nullable String title, @Nullable String reason, @NotNull MaintenanceType status, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, int createdBy);

    @Nullable Maintenance update(int id, @Nullable String title, @Nullable String reason, @NotNull MaintenanceType status, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, int changedBy);
}
