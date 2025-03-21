package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.group.Group;

import java.sql.Timestamp;
import java.util.List;

/**
 * User parent interface to manage user parents.
 */
public sealed interface UserParent permits UserParentProvider {
    /**
     * Checks if a parent-child relationship exists between a user and a parent.
     *
     * @param userId   The ID of the user whose parent relationship is to be verified
     * @param parentId The ID of the parent to be checked
     * @return True if the user has the specified parent, false otherwise
     */
    boolean existsParent(int userId, int parentId);

    /**
     * Adds a parent-child relationship for the specified user with an optional expiration time.
     *
     * @param executorId The ID of the user performing the log action.
     * @param userId     The ID of the user for whom the parent relationship is being added.
     * @param parentId   The ID of the parent to be associated with the user.
     * @param time       The duration (in milliseconds) until this relationship expires.
     *                   Use -1 for a relationship that does not expire.
     */
    void addParent(int executorId, int userId, int parentId, long time);

    /**
     * Removes the parent-child relationship between a specified user and parent.
     *
     * @param userId   The ID of the user whose parent relationship is being removed.
     * @param parentId The ID of the parent to be removed from the user.
     */
    void removeParent(int userId, int parentId);

    /**
     * Clears all parent-child relationships for the specified user.
     *
     * @param userId The ID of the user whose parent relationships are being cleared.
     */
    void clearParent(int userId);

    /**
     * Retrieves the list of parent IDs associated with the specified user.
     *
     * @param userId The ID of the user whose parent IDs are to be fetched.
     * @return A list of integers representing the parent IDs associated with the user.
     */
    List<Integer> getParentIds(int userId);

    /**
     * Retrieves the list of parent names associated with the specified user within the given group.
     *
     * @param group  The group context in which the parent names are to be retrieved.
     * @param userId The ID of the user whose parent names are to be fetched.
     * @return A list of strings representing the names of the parents associated with the specified user.
     */
    List<String> getParentNames(Group group, int userId);

    /**
     * Retrieves the highest priority value for a specific user within the given group.
     *
     * @param group  The group context to retrieve the priority for the user.
     * @param userId The ID of the user whose highest priority is being determined.
     * @return The highest priority value associated with the user in the specified group.
     */
    int getHighestPriority(Group group, int userId);

    /**
     * Retrieves the expiration timestamp of the relationship between the specified user and parent.
     *
     * @param userId   The ID of the user whose relationship expiration time is being retrieved.
     * @param parentId The ID of the parent involved in the relationship.
     * @return A {@code Timestamp} object representing the expiration date and time of the association.
     */
    Timestamp getExpiredAt(int userId, int parentId);

    /**
     * Sets the expiration time for the relationship between a specified user and parent.
     *
     * @param executorId The ID of the user performing the log action.
     * @param userId     The ID of the user whose parent relationship is being updated.
     * @param parentId   The ID of the parent associated with the user.
     * @param time       The new expiration timestamp for the relationship in milliseconds since the epoch.
     *                   Use -1 for a relationship that does not expire.
     */
    void setExpiredAt(int executorId, int userId, int parentId, long time);

    /**
     * Retrieves the ID of the user who created the parent-child relationship
     * between the specified user and parent.
     *
     * @param userId   The ID of the user associated with the relationship.
     * @param parentId The ID of the parent associated with the relationship.
     * @return The ID of the user who created the relationship.
     */
    int getCreatedBy(int userId, int parentId);

    /**
     * Retrieves the timestamp of when the parent-child relationship between a specified user and parent was created.
     *
     * @param userId   The ID of the user whose parent relationship creation timestamp is being retrieved.
     * @param parentId The ID of the parent involved in the relationship.
     * @return The timestamp representing the creation time of the relationship.
     */
    Timestamp getCreatedAt(int userId, int parentId);

    /**
     * Retrieves the ID of the user who last modified the relationship between the specified user and parent.
     *
     * @param userId   The ID of the user associated with the relationship.
     * @param parentId The ID of the parent associated with the relationship.
     * @return The ID of the user who last modified the relationship.
     */
    int getModifiedBy(int userId, int parentId);

    /**
     * Retrieves the timestamp indicating the last modification time
     * of the relationship between the specified user and parent.
     *
     * @param userId   The ID of the user whose relationship modification timestamp is being retrieved.
     * @param parentId The ID of the parent involved in the relationship.
     * @return A {@code Timestamp} object representing the last modification time
     * of the relationship between the user and parent.
     */
    Timestamp getModifiedAt(int userId, int parentId);

    /**
     * Loads and processes the expired parent-child relationships for a given user.
     */
    void loadExpired();
}
