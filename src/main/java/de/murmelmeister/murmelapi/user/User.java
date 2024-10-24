package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.time.JoinLogger;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.time.QuitLogger;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.settings.UserSettings;

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
     */
    boolean existsUser(UUID uuid);

    /**
     * Checks if a user exists.
     *
     * @param username The username of the user.
     * @return True if the user exists, otherwise false.
     */
    boolean existsUser(String username);

    /**
     * Create a new user and check if the user already exists.
     * If the user already exists, the method will return without creating a new user.
     *
     * @param uuid     The unique id of the user.
     * @param username The username of the user.
     */
    void createNewUser(UUID uuid, String username);

    /**
     * Deletes a user.
     *
     * @param uuid The unique id of the user.
     */
    void deleteUser(UUID uuid);

    /**
     * Obtains the id of a user.
     *
     * @param uuid The unique id of the user.
     * @return The id of the user.
     */
    int getId(UUID uuid);

    /**
     * Obtains the id of a user.
     *
     * @param username The username of the user.
     * @return The id of the user.
     */
    int getId(String username);

    /**
     * Obtains the unique id of a user.
     *
     * @param username The username of the user.
     * @return The unique id of the user.
     */
    UUID getUniqueId(String username);

    /**
     * Obtains the unique id of a user.
     *
     * @param id The id of the user.
     * @return The unique id of the user.
     */
    UUID getUniqueId(int id);

    /**
     * Obtains the username of a user.
     *
     * @param uuid The unique id of the user.
     * @return The username of the user.
     */
    String getUsername(UUID uuid);

    /**
     * Obtains the username of a user.
     * If the id -1 then the method will return "CONSOLE".
     *
     * @param id The id of the user.
     * @return The username of the user.
     */
    String getUsername(int id);

    /**
     * Renames a user.
     *
     * @param uuid    The unique id of the user.
     * @param newName The new username of the user.
     */
    void rename(UUID uuid, String newName);

    /**
     * Obtains a list of all unique ids.
     *
     * @return A list of all unique ids.
     */
    List<UUID> getUniqueIds();

    /**
     * Obtains a list of all usernames.
     *
     * @return A list of all usernames.
     */
    List<String> getUsernames();

    /**
     * Obtains a list of all ids.
     *
     * @return A list of all ids.
     */
    List<Integer> getIds();

    /**
     * Join a user to the server.
     * Create a new user if the user does not exist.
     * Check if the user changes their name and rename them.
     *
     * @param uuid     The unique id of the user.
     * @param username The username of the user.
     */
    void joinUser(UUID uuid, String username);

    /**
     * Load all expired things.
     */
    void loadExpired();

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

    /**
     * Obtains the play time of a user.
     *
     * @return The play time of the user.
     */
    PlayTime getPlayTime();

    /**
     * Obtains the JoinLogger instance for managing and logging the join dates of users.
     *
     * @return The JoinLogger instance associated with the user.
     */
    JoinLogger getJoinLogger();

    /**
     * Obtains the QuitLogger instance for managing and logging the quit dates of users.
     *
     * @return The QuitLogger instance associated with the user.
     */
    QuitLogger getQuitLogger();
}
