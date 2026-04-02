package de.murmelmeister.murmelapi.permission.parent;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface ParentProvider {
    void refreshCache();

    @NotNull Optional<Parent> findParent(@NotNull PermissionTarget target, int parentId);

    @NotNull
    @Unmodifiable
    List<Parent> findParents(@NotNull PermissionTarget target);

    @NotNull Optional<Parent> upsert(@NotNull PermissionTarget target, int parentId, long duration, int executorId);

    int remove(@NotNull PermissionTarget target, int parentId);

    int clear(@NotNull PermissionTarget target);

    int loadExpired();

    @ApiStatus.Internal
    static @NotNull ParentProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new ParentProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
