package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * User interface to manage users.
 */
public sealed interface User permits UserProvider {
    /**
     * Creates a new user with the specified username if it does not already exist,
     * or retrieves the ID of an existing user with the specified username.
     *
     * @param username The username of the user to create or retrieve
     * @return The unique numeric ID of the newly created or existing user
     */
    int createOrGetUser(String username);

    /**
     * Creates a new user with the specified UUID if it does not already exist,
     * or retrieves the ID of an existing user associated with the specified UUID asynchronously.
     *
     * @param uuid The unique identifier of the user to create or retrieve
     * @return A CompletableFuture that will complete with the unique numeric ID of the user
     */
    CompletableFuture<Integer> createOrGetUserAsync(UUID uuid);

    /**
     * Creates a new user with the specified UUID if it does not already exist,
     * or retrieves the ID of an existing user associated with the specified UUID.
     *
     * @param uuid The unique identifier of the user to create or retrieve
     * @return The unique numeric ID of the newly created or existing user
     */
    int createOrGetUser(UUID uuid);

    /**
     * Creates a new user with the specified username if it does not already exist,
     * or retrieves the ID of an existing user with the specified username asynchronously.
     *
     * @param username The username of the user to create or retrieve
     * @return A CompletableFuture that will complete with the unique numeric ID of the user
     */
    CompletableFuture<Integer> createOrGetUserAsync(String username);

    /**
     * Checks if a user with the specified UUID exists.
     *
     * @param uuid The unique identifier of the user to check
     * @return True if a user with the given UUID exists, false otherwise
     */
    boolean existsUser(UUID uuid);

    /**
     * Checks if a user with the specified UUID exists asynchronously.
     *
     * @param uuid The unique identifier of the user to check
     * @return A CompletableFuture that will complete with true if the user exists, false otherwise
     */
    CompletableFuture<Boolean> existsUserAsync(UUID uuid);

    /**
     * Checks if a user with the specified username exists.
     *
     * @param username The username of the user to check
     * @return True if a user with the given username exists, false otherwise
     */
    boolean existsUser(String username);

    /**
     * Checks if a user with the specified username exists asynchronously.
     *
     * @param username The username of the user to check
     * @return A CompletableFuture that will complete with true if the user exists, false otherwise
     */
    CompletableFuture<Boolean> existsUserAsync(String username);

    /**
     * Creates a new user in the system based on the provided UUID and username.
     * If the user already exists, no new user will be created.
     *
     * @param uuid     The unique identifier of the user to be created
     * @param username The username of the user to be created
     */
    void createUser(UUID uuid, String username);

    /**
     * Creates a new user in the system based on the provided UUID and username asynchronously.
     * If the user already exists, no new user will be created.
     *
     * @param uuid     The unique identifier of the user to be created
     * @param username The username of the user to be created
     * @return A CompletableFuture that will complete when the user has been created
     */
    CompletableFuture<Integer> createUserAsync(UUID uuid, String username);

    /**
     * Deletes a user associated with the specified UUID.
     *
     * @param uuid The unique identifier of the user to be deleted
     */
    void deleteUser(UUID uuid);

    /**
     * Deletes a user associated with the specified UUID asynchronously.
     *
     * @param uuid The unique identifier of the user to be deleted
     * @return A CompletableFuture that will complete when the user has been deleted
     */
    CompletableFuture<Integer> deleteUserAsync(UUID uuid);

    /**
     * Retrieves the unique numeric ID of a user associated with the specified UUID.
     *
     * @param uuid The unique identifier of the user whose ID is to be retrieved
     * @return The unique numeric ID of the user associated with the given UUID
     */
    int getId(UUID uuid);

    /**
     * Retrieves the unique numeric ID of a user associated with the specified UUID asynchronously.
     *
     * @param uuid The unique identifier of the user whose ID is to be retrieved
     * @return A CompletableFuture that will complete with the unique numeric ID of the user
     */
    CompletableFuture<Integer> getIdAsync(UUID uuid);

    /**
     * Retrieves the unique numeric ID of a user associated with the specified username.
     *
     * @param username The username of the user whose ID is to be retrieved
     * @return The unique numeric ID of the user associated with the given username
     */
    int getId(String username);

    /**
     * Retrieves the unique numeric ID of a user associated with the specified username asynchronously.
     *
     * @param username The username of the user whose ID is to be retrieved
     * @return A CompletableFuture that will complete with the unique numeric ID of the user
     */
    CompletableFuture<Integer> getIdAsync(String username);

    /**
     * Retrieves the unique identifier (UUID) associated with the specified user ID.
     * If no user exists with the given ID, the behavior may vary depending on the implementation.
     *
     * @param userId The unique numeric ID of the user
     * @return The UUID associated with the specified user ID
     */
    UUID getUniqueId(int userId);

    /**
     * Retrieves the unique identifier (UUID) associated with the specified user ID asynchronously.
     *
     * @param userId The unique numeric ID of the user
     * @return A CompletableFuture that will complete with the UUID associated with the specified user ID
     */
    CompletableFuture<UUID> getUniqueIdAsync(int userId);

    /**
     * Retrieves the unique identifier (UUID) associated with the specified username.
     * If no user exists with the given username, the behavior may vary depending on the implementation.
     *
     * @param username The username of the user whose UUID is to be retrieved
     * @return The UUID associated with the specified username
     */
    UUID getUniqueId(String username);

    /**
     * Retrieves the unique identifier (UUID) associated with the specified username asynchronously.
     *
     * @param username The username of the user whose UUID is to be retrieved
     * @return A CompletableFuture that will complete with the UUID associated with the specified username
     */
    CompletableFuture<UUID> getUniqueIdAsync(String username);

    /**
     * Retrieves the username associated with the specified user ID.
     *
     * @param userId The unique numeric ID of the user whose username is to be retrieved
     * @return The username associated with the given user ID
     */
    String getUsername(int userId);

    /**
     * Retrieves the username associated with the specified user ID asynchronously.
     *
     * @param userId The unique numeric ID of the user whose username is to be retrieved
     * @return A CompletableFuture that will complete with the username associated with the given user ID
     */
    CompletableFuture<String> getUsernameAsync(int userId);

    /**
     * Retrieves the username associated with the specified UUID.
     *
     * @param uuid The unique identifier of the user whose username is to be retrieved
     * @return The username associated with the given UUID
     */
    String getUsername(UUID uuid);

    /**
     * Retrieves the username associated with the specified UUID asynchronously.
     *
     * @param uuid The unique identifier of the user whose username is to be retrieved
     * @return A CompletableFuture that will complete with the username associated with the given UUID
     */
    CompletableFuture<String> getUsernameAsync(UUID uuid);

    /**
     * Renames a user by updating the username associated with the given user ID.
     *
     * @param userId      The unique numeric ID of the user whose username is to be updated
     * @param newUsername The new username to associate with the user
     */
    void rename(int userId, String newUsername);

    /**
     * Renames a user by updating the username associated with the given user ID asynchronously.
     *
     * @param userId      The unique numeric ID of the user whose username is to be updated
     * @param newUsername The new username to associate with the user
     */
    CompletableFuture<Integer> renameAsync(int userId, String newUsername);

    /**
     * Updates the username of the user associated with the specified UUID.
     *
     * @param uuid        The unique identifier of the user to be updated
     * @param newUsername The new username to assign to the user
     */
    void rename(UUID uuid, String newUsername);

    /**
     * Updates the username of the user associated with the specified UUID asynchronously.
     *
     * @param uuid        The unique identifier of the user to be updated
     * @param newUsername The new username to assign to the user
     */
    CompletableFuture<Integer> renameAsync(UUID uuid, String newUsername);

    /**
     * Retrieves a list of unique identifiers (UUIDs) associated with all users.
     *
     * @return A list of UUIDs representing all users.
     */
    List<UUID> getUniqueIds();

    /**
     * Retrieves a list of unique identifiers (UUIDs) associated with all users asynchronously.
     *
     * @return A CompletableFuture that will complete with a list of UUIDs representing all users.
     */
    CompletableFuture<List<UUID>> getUniqueIdsAsync();

    /**
     * Retrieves a list of usernames for all users.
     *
     * @return A list of usernames representing all users.
     */
    List<String> getUsernames();

    /**
     * Retrieves a list of usernames for all users asynchronously.
     *
     * @return A CompletableFuture that will complete with a list of usernames representing all users.
     */
    CompletableFuture<List<String>> getUsernamesAsync();

    /**
     * Retrieves the first join time of a user in milliseconds since the epoch.
     *
     * @param userId The unique numeric ID of the user
     * @return The first join time of the user in milliseconds since January 1, 1970, 00:00:00 GMT
     */
    Timestamp getFirstJoinTime(int userId);

    /**
     * Retrieves the first join time of a user as a {@code Timestamp} object.
     *
     * @param userId The unique numeric ID of the user whose first join time is to be retrieved
     * @return A {@code Timestamp} object representing the first join time of the user
     */
    CompletableFuture<Timestamp> getFirstJoinTimeAsync(int userId);

    /**
     * Retrieves the first join date of a user as a formatted date string.
     *
     * @param userId The unique numeric ID of the user whose first join date is to be retrieved
     * @return A string representing the first join date of the user
     */
    String getFirstJoinDate(int userId);

    /**
     * Retrieves the first join date of a user as a formatted date string asynchronously.
     *
     * @param userId The unique numeric ID of the user whose first join date is to be retrieved
     * @return A CompletableFuture that will complete with the first join date of the user
     */
    CompletableFuture<String> getFirstJoinDateAsync(int userId);

    /**
     * Sets the first join time for a user identified by the given user ID.
     *
     * @param userId The unique numeric ID of the user whose first join time is to be set
     */
    void setFirstJoinTime(int userId);

    /**
     * Sets the first join time for a user identified by the given user ID asynchronously.
     *
     * @param userId The unique numeric ID of the user whose first join time is to be set
     * @return A CompletableFuture that will complete when the first join time has been set
     */
    CompletableFuture<Integer> setFirstJoinTimeAsync(int userId);

    /**
     * Joins the user identified by the specified UUID and username into the system.
     *
     * @param uuid     The unique identifier associated with the user to join
     * @param username The username of the user to join
     */
    void joinUser(UUID uuid, String username);

    /**
     * Joins the user identified by the specified UUID and username into the system asynchronously.
     *
     * @param uuid     The unique identifier associated with the user to join
     * @param username The username of the user to join
     * @return A CompletableFuture that will complete when the user has been joined
     */
    CompletableFuture<Void> joinUserAsync(UUID uuid, String username);

    /**
     * Loads and processes expired user data in the system.
     * This method handles expired or outdated user records, executing
     * relevant operations such as cleanup, refresh, or other maintenance tasks
     * depending on the specific implementation.
     */
    void loadExpired();

    /**
     * Retrieves the parent information associated with the user.
     *
     * @return A UserParent object representing the parent information of the user.
     */
    UserParent getParent();

    /**
     * Retrieves the permission associated with a user.
     *
     * @return The UserPermission object representing the user's permissions.
     */
    UserPermission getPermission();
}
