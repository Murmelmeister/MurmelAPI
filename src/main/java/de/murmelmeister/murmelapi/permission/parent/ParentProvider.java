package de.murmelmeister.murmelapi.permission.parent;

import de.murmelmeister.murmelapi.permission.PermissionTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public interface ParentProvider {
    void refreshCache();

    @NotNull Optional<Parent> findParent(@NotNull PermissionTarget target, int parentId);

    @NotNull @Unmodifiable
    List<Parent> findParents(@NotNull PermissionTarget target);

    @NotNull Optional<Parent> upsert(@NotNull PermissionTarget target, int parentId, long duration, int executorId);

    int remove(@NotNull PermissionTarget target, int parentId);

    int clear(@NotNull PermissionTarget target);

    int loadExpired();
}
