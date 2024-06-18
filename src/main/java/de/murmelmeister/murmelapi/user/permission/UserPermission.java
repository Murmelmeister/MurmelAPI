package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.murmelapi.user.User;

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
     */
    boolean existsPermission(int userId, String permission);

    /**
     * Adds a permission to a user.
     *
     * @param userId     The id of the user.
     * @param creatorId  The id of the creator.
     * @param permission The permission.
     * @param time       The time the permission was added.
     */
    void addPermission(int userId, int creatorId, String permission, long time);

    /**
     * Removes a permission from a user.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     */
    void removePermission(int userId, String permission);

    /**
     * Clears all permissions from a user.
     *
     * @param userId The id of the user.
     */
    void clearPermission(int userId);

    /**
     * Obtains all permissions of a user.
     *
     * @param userId The id of the user.
     * @return A list of all permissions of the user.
     */
    List<String> getPermissions(int userId);

    /**
     * Obtains the creator id of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The creator id of the permission.
     */
    int getCreatorId(int userId, String permission);

    /**
     * Obtains the created time of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The created time of the permission.
     */
    long getCreatedTime(int userId, String permission);

    /**
     * Obtains the created date of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The created date of the permission.
     */
    String getCreatedDate(int userId, String permission);

    /**
     * Obtains the expired time of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The expired time of the permission.
     */
    long getExpiredTime(int userId, String permission);

    /**
     * Obtains the expired date of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @return The expired date of the permission.
     */
    String getExpiredDate(int userId, String permission);

    /**
     * Sets the expired time of a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @param time       The time the permission will expire.
     * @return The expired date of the permission.
     */
    String setExpiredTime(int userId, String permission, long time);

    /**
     * Adds an expired time to a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @param time       The time to add to the expired time.
     * @return The expired date of the permission.
     */
    String addExpiredTime(int userId, String permission, long time);

    /**
     * Removes an expired time from a permission.
     *
     * @param userId     The id of the user.
     * @param permission The permission.
     * @param time       The time to remove from the expired time.
     * @return The expired date of the permission.
     */
    String removeExpiredTime(int userId, String permission, long time);

    /**
     * Loads all expired permissions of a user.
     *
     * @param user The user.
     */
    void loadExpired(User user);
}
