package de.murmelmeister.murmelapi.permission;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface PermissionProvider {
    void refreshCache();

    @NotNull Optional<Permission> findPermission(@NotNull PermissionTarget target, @NotNull String permission);

    @NotNull
    @Unmodifiable
    List<Permission> findPermissions(@NotNull PermissionTarget target);

    @NotNull Optional<Permission> upsert(@NotNull PermissionTarget target, @NotNull String permission, long duration, int executorId);

    int remove(@NotNull PermissionTarget target, @NotNull String permission);

    int clear(@NotNull PermissionTarget target);

    int loadExpired();

    @ApiStatus.Internal
    static @NotNull PermissionProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new PermissionProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
