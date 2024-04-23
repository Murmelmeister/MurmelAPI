package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.settings.UserSettings;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * User interface to manage users.
 */
public sealed interface User permits UserProvider {
    /**
     * Checks if a user exists.
     *
     * @param uuid The unique id of the user.
     * @return True if the user exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsUser(UUID uuid) throws SQLException;

    /**
     * Checks if a user exists.
     *
     * @param username The username of the user.
     * @return True if the user exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsUser(String username) throws SQLException;

    /**
     * Create a new user and check if the user already exists.
     * If the user already exists, the method will return without creating a new user.
     *
     * @param uuid     The unique id of the user.
     * @param username The username of the user.
     * @throws SQLException If an SQL error occurs.
     */
    void createNewUser(UUID uuid, String username) throws SQLException;

    /**
     * Deletes a user.
     *
     * @param uuid The unique id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    void deleteUser(UUID uuid) throws SQLException;

    /**
     * Obtains the id of a user.
     *
     * @param uuid The unique id of the user.
     * @return The id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    int getId(UUID uuid) throws SQLException;

    /**
     * Obtains the id of a user.
     *
     * @param username The username of the user.
     * @return The id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    int getId(String username) throws SQLException;

    /**
     * Obtains the unique id of a user.
     *
     * @param username The username of the user.
     * @return The unique id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    UUID getUniqueId(String username) throws SQLException;

    /**
     * Obtains the unique id of a user.
     *
     * @param id The id of the user.
     * @return The unique id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    UUID getUniqueId(int id) throws SQLException;

    /**
     * Obtains the username of a user.
     *
     * @param uuid The unique id of the user.
     * @return The username of the user.
     * @throws SQLException If an SQL error occurs.
     */
    String getUsername(UUID uuid) throws SQLException;

    /**
     * Renames a user.
     *
     * @param uuid    The unique id of the user.
     * @param newName The new username of the user.
     * @throws SQLException If an SQL error occurs.
     */
    void rename(UUID uuid, String newName) throws SQLException;

    /**
     * Obtains a list of all unique ids.
     *
     * @return A list of all unique ids.
     * @throws SQLException If an SQL error occurs.
     */
    List<UUID> getUniqueIds() throws SQLException;

    /**
     * Obtains a list of all usernames.
     *
     * @return A list of all usernames.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getUsernames() throws SQLException;

    /**
     * Obtains a list of all ids.
     *
     * @return A list of all ids.
     * @throws SQLException If an SQL error occurs.
     */
    List<Integer> getIds() throws SQLException;

    /**
     * Join a user to the server.
     * Create a new user if the user does not exist.
     * Check if the user changes their name and rename them.
     *
     * @param uuid     The unique id of the user.
     * @param username The username of the user.
     * @throws SQLException If an SQL error occurs.
     */
    void joinUser(UUID uuid, String username) throws SQLException;

    /**
     * Load all expired things.
     *
     * @throws SQLException If an SQL error occurs.
     */
    void loadExpired() throws SQLException;

    /**
     * Obtains the settings of a user.
     *
     * @return The settings of the user.
     */
    UserSettings getSettings();

    /**
     * Obtains the parent of a user.
     *
     * @return The parent of the user.
     */
    UserParent getParent();

    /**
     * Obtains the permission of a user.
     *
     * @return The permission of the user.
     */
    UserPermission getPermission();
}
