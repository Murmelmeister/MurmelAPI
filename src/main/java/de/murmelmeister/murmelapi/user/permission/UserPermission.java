package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.murmelapi.user.User;

import java.sql.SQLException;
import java.util.List;

/**
 * User permission interface to manage user permissions.
 */
public sealed interface UserPermission permits UserPermissionProvider {
    /**
     * Checks if a permission exists.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return True if the permission exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsPermission(int userId, String permission) throws SQLException;

    /**
     * Adds a permission to a user.
     *
     * @param userId     The id of the user.
     * @param creatorId  The id of the creator.
     * @param permission The permission.
     * @param time       The time the permission was added.
     * @throws SQLException If an SQL error occurs.
     */
    void addPermission(int userId, int creatorId, String permission, long time) throws SQLException;

    /**
     * Removes a permission from a user.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @throws SQLException If an SQL error occurs.
     */
    void removePermission(int userId, String permission) throws SQLException;

    /**
     * Clears all permissions from a user.
     *
     * @param userId The id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    void clearPermission(int userId) throws SQLException;

    /**
     * Obtains all permissions of a user.
     *
     * @param userId The id of the user.
     * @return A list of all permissions of the user.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getPermissions(int userId) throws SQLException;

    /**
     * Obtains the creator id of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The creator id of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    int getCreatorId(int userId, String permission) throws SQLException;

    /**
     * Obtains the created time of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The created time of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    long getCreatedTime(int userId, String permission) throws SQLException;

    /**
     * Obtains the created date of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The created date of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    String getCreatedDate(int userId, String permission) throws SQLException;

    /**
     * Obtains the expired time of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The expired time of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    long getExpiredTime(int userId, String permission) throws SQLException;

    /**
     * Obtains the expired date of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The expired date of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    String getExpiredDate(int userId, String permission) throws SQLException;

    /**
     * Sets the expired time of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @param time       The time the permission will expire.
     * @return The expired date of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    String setExpiredTime(int userId, String permission, long time) throws SQLException;

    /**
     * Adds an expired time to a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @param time       The time to add to the expired time.
     * @return The expired date of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    String addExpiredTime(int userId, String permission, long time) throws SQLException;

    /**
     * Removes an expired time from a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @param time       The time to remove from the expired time.
     * @return The expired date of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    String removeExpiredTime(int userId, String permission, long time) throws SQLException;

    /**
     * Loads all expired permissions of a user.
     *
     * @param user The user.
     * @throws SQLException If an SQL error occurs.
     */
    void loadExpired(User user) throws SQLException;
}
