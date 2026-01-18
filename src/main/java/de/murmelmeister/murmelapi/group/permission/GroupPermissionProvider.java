package de.murmelmeister.murmelapi.group.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GroupPermission is a class that provides methods to manage group permissions in the database.
 * It implements the GroupPermissionProvider interface and uses the Database class to interact with the database.
 */
public sealed interface GroupPermissionProvider permits GroupPermissionProviderImpl {
    void refreshCache();

    @Nullable GroupPermission getPermission(int groupId, @NotNull String permission);

    @Nullable List<GroupPermission> getPermissions(int groupId);

    @Nullable GroupPermission add(int groupId, @NotNull String permission, long duration, int createdBy);

    int remove(int groupId, @NotNull String permission);

    int clear(int groupId);

    @Nullable GroupPermission update(int groupId, @NotNull String permission, long duration, int changedBy);

    int loadExpired();
}
