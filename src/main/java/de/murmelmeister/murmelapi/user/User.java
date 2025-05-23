package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * The {@code User} interface provides methods for managing user data in a database.
 * It allows checking for user existence, creating and deleting users, and retrieving user information.
 * This interface is designed to be implemented by classes that provide specific database interactions.
 */
public sealed interface User permits UserProvider {
    /**
     * Checks if a user exists by their UUID.
     *
     * @param uuid The unique identifier (UUID) of the user.
     * @return {@code true} if the UUID is not null and a corresponding record exists in the database; {@code false} otherwise.
     */
    boolean existsUser(UUID uuid);

    /**
     * Checks if a user exists by their username.
     *
     * @param username The username of the user.
     * @return {@code true} if the username is not null and a corresponding record exists in the database; {@code false} otherwise.
     */
    boolean existsUser(String username);

    /**
     * Creates a new user record in the database.
     *
     * @param uuid     The unique identifier (UUID) of the new user.
     * @param username The username of the new user.
     * @return The number of rows updated in the database; returns 0 if either {@code uuid} or {@code username} is null.
     */
    int createUser(UUID uuid, String username);

    /**
     * Deletes a user record from the database.
     *
     * @param id The unique identifier (ID) of the user to be deleted.
     * @return The number of rows updated in the database; returns 0 if the {@code id} is less than 1.
     */
    int deleteUser(int id);

    /**
     * Retrieves the user ID based on the given UUID.
     *
     * @param uuid The unique identifier (UUID) of the user.
     * @return The user ID if found; returns -2 if {@code uuid} is null or if no corresponding record exists.
     */
    int getId(UUID uuid);

    /**
     * Retrieves the user ID based on the given username.
     *
     * @param username The username of the user.
     * @return The user ID if found; returns -2 if {@code username} is null or if no corresponding record exists.
     */
    int getId(String username);

    /**
     * Retrieves the UUID associated with a given user ID.
     *
     * @param id The unique identifier (ID) of the user.
     * @return The user's UUID if found; returns {@code null} if {@code id} is less than 1.
     */
    UUID getUniqueId(int id);

    /**
     * Retrieves the username associated with a given user ID.
     *
     * @param id The unique identifier (ID) of the user.
     * @return The username if found; returns "Console" if {@code id} equals -1, {@code null} if {@code id} is less than 1,
     * or the corresponding username from the database.
     */
    String getUsername(int id);

    /**
     * Renames the user by updating their username in the database.
     *
     * @param id       The unique identifier (ID) of the user.
     * @param username The new username for the user.
     * @return The number of rows updated in the database; returns 0 if {@code id} is less than 1 or {@code username} is null.
     */
    int renameUser(int id, String username);


    /**
     * Retrieves a list of all user UUIDs from the database.
     *
     * @return A {@code List} of UUIDs; any {@code null} values are filtered out.
     */
    List<UUID> getUniqueIds();

    /**
     * Retrieves a list of all usernames from the database.
     *
     * @return A {@code List} of usernames; any {@code null} values are filtered out.
     */
    List<String> getUsernames();

    /**
     * Retrieves the timestamp representing the user's first join date from the database.
     *
     * @param id The unique identifier (ID) of the user.
     * @return A {@code Timestamp} indicating the first join date; returns {@code null} if {@code id} is less than 1.
     */
    Timestamp getFirstJoin(int id);

    /**
     * Formats and returns the first join date as a {@code String}.
     *
     * @param id The unique identifier (ID) of the user.
     * @return A formatted date string representing the first join date, or "never" if the timestamp is {@code null}.
     */
    String getFirstJoinDate(int id);

    /**
     * Updates the first join timestamp for a user in the database.
     *
     * @param id        The unique identifier (ID) of the user.
     * @param firstJoin The {@code Timestamp} representing the first join date.
     * @return The number of rows updated in the database; returns 0 if {@code id} is less than 1 or {@code firstJoin} is {@code null}.
     */
    int setFirstJoin(int id, Timestamp firstJoin);

    /**
     * Checks if a user is flagged as a debug user.
     *
     * @param id The unique identifier (ID) of the user.
     * @return {@code true} if the user has a debug flag set in the database and {@code id} is greater than 0; {@code false} otherwise.
     */
    boolean isDebugUser(int id);

    /**
     * Sets or unsets the debug flag for a user.
     *
     * @param id          The unique identifier (ID) of the user.
     * @param isDebugUser {@code true} to set the user as a debug user, {@code false} otherwise.
     * @return The number of rows updated in the database; returns 0 if {@code id} is less than 1.
     */
    int setDebugUser(int id, boolean isDebugUser);

    /**
     * Checks if the debug mode is active for a user.
     *
     * @param id The unique identifier (ID) of the user.
     * @return {@code true} if the debug mode is active for the user and {@code id} is greater than 0; {@code false} otherwise.
     */
    boolean isDebugActive(int id);

    /**
     * Sets the active status of debug mode for a user.
     *
     * @param id            The unique identifier (ID) of the user.
     * @param isDebugActive {@code true} to activate debug mode for the user, {@code false} to deactivate.
     * @return The number of rows updated in the database; returns 0 if {@code id} is less than 1.
     */
    int setDebugActive(int id, boolean isDebugActive);

    /**
     * Determines if a user is currently in debug mode by checking if they are both a debug user
     * and have debug mode active.
     *
     * @param id The unique identifier (ID) of the user.
     * @return {@code true} if both the debug user flag and debug active flag are set; {@code false} otherwise.
     */
    boolean isDebugMode(int id);

    /**
     * Retrieves the language ID associated with a given user ID.
     *
     * @param id The unique identifier of the user; must be ≥ 1
     * @return The language ID for the user; returns -1 if {@code id} < 1
     * or if no matching record exists
     */
    int getLanguage(int id);

    /**
     * Updates the language ID for a given user ID.
     *
     * @param id       The unique identifier (ID) of the user; must not be -2.
     * @param language The new language ID to assign; must be greater than zero.
     * @return 1 if parameters are invalid ({@code id} == -2 or {@code language} < 1);
     * otherwise the number of rows affected by the update.
     */
    int setLanguage(int id, int language);

    /**
     * Manages the process for a user joining the system. This method:
     * <ul>
     *   <li>Retrieves the user ID for the given UUID.</li>
     *   <li>If the user is not found (indicated by a special value), it creates a new user record.</li>
     *   <li>If the username does not match the current one in the database, it updates the username.</li>
     *   <li>Returns 0 if no changes are necessary or if input values are invalid.</li>
     * </ul>
     *
     * @param uuid     The unique identifier (UUID) of the user joining.
     * @param username The username of the user joining.
     * @return The result of the performed database update operation (e.g., number of rows updated),
     * or 0 if the operation was not applicable.
     */
    int joinUser(UUID uuid, String username);

    /**
     * Removes all permission and parents records that have expired.
     */
    int loadExpired();

    /**
     * Retrieves the {@code UserParent} instance associated with this user.
     *
     * @return The {@code UserParent} instance.
     */
    UserParent getParent();

    /**
     * Retrieves the {@code UserPermission} instance associated with this user.
     *
     * @return The {@code UserPermission} instance.
     */
    UserPermission getPermission();
}
