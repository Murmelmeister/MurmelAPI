package de.murmelmeister.murmelapi.maintenance.whitelist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.LocalDateTime;
import java.util.List;

public interface MaintenanceWhitelistProvider {
    void refreshCache();

    @Nullable MaintenanceWhitelist findById(int id);

    @NotNull
    @Unmodifiable
    List<MaintenanceWhitelist> findByMaintenanceId(int maintenanceId);

    @NotNull @Unmodifiable List<MaintenanceWhitelist> findByUserId(int userId);

    @NotNull @Unmodifiable List<MaintenanceWhitelist> findAll();

    @Nullable MaintenanceWhitelist create(int maintenanceId, int userId, @Nullable LocalDateTime startAt, @Nullable LocalDateTime endAt, @Nullable String note, int createdBy);

    int delete(int id);

    @Nullable MaintenanceWhitelist update(int id, @Nullable LocalDateTime startAt, @Nullable LocalDateTime endAt, @Nullable String note, int changedBy);
}
