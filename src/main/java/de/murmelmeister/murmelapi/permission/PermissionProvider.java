package de.murmelmeister.murmelapi.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

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
}
