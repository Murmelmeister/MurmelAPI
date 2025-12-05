package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.user.User;

import java.util.Set;
import java.util.UUID;

/**
 * The Permission interface provides methods to manage and check user permissions.
 * It is designed to be implemented by classes that handle permission management.
 */
public sealed interface Permission permits PermissionProvider {
    Set<String> getPermissions(int userId);

    boolean hasPermission(User user, String permission);

    boolean hasPermission(UUID uuid, String permission);

    int loadExpired();
}
