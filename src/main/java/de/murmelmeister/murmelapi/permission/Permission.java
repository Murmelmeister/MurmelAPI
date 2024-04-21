package de.murmelmeister.murmelapi.permission;

import java.sql.SQLException;
import java.util.List;

/**
 * Permission interface to manage permissions.
 */
public sealed interface Permission permits PermissionProvider {
    /**
     * Obtains the permissions of a user.
     *
     * @param userId The id of the user.
     * @return The permissions of the user.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getPermissions(int userId) throws SQLException;

    /**
     * Checks if a user has a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return True if the user has the permission, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean hasPermission(int userId, String permission) throws SQLException;
}
