package de.murmelmeister.murmelapi.maintenance.whitelist;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MaintenanceWhitelistProvider {
    void refreshCache();

    @NotNull Optional<MaintenanceWhitelist> findById(int id);

    @NotNull
    @Unmodifiable
    List<MaintenanceWhitelist> findByMaintenanceId(int maintenanceId);

    @NotNull
    @Unmodifiable
    List<MaintenanceWhitelist> findByUserId(int userId);

    @NotNull
    @Unmodifiable
    List<MaintenanceWhitelist> findAll();

    @NotNull Optional<MaintenanceWhitelist> create(int maintenanceId, int userId, @Nullable LocalDateTime startAt, @Nullable LocalDateTime endAt, @Nullable String note, int createdBy);

    int delete(int id);

    @NotNull Optional<MaintenanceWhitelist> update(int id, @Nullable LocalDateTime startAt, @Nullable LocalDateTime endAt, @Nullable String note, int changedBy);

    @ApiStatus.Internal
    static @NotNull MaintenanceWhitelistProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new MaintenanceWhitelistProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
