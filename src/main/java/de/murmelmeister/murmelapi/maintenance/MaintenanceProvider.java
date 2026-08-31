package de.murmelmeister.murmelapi.maintenance;

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
import java.util.function.Consumer;

public interface MaintenanceProvider {
    void refreshCache();

    @NotNull Optional<Maintenance> findById(int id);

    @NotNull
    @Unmodifiable
    List<Maintenance> findAll();

    @NotNull Optional<Maintenance> findActive();

    @NotNull Optional<Maintenance> create(int createdBy, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String title, @Nullable String reason);

    @NotNull Optional<Maintenance> update(int id, int changedBy, @NotNull Consumer<Maintenance.Builder> updater);

    @ApiStatus.Internal
    static @NotNull MaintenanceProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new MaintenanceProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
