package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;

import java.util.List;

/**
 * User parent interface to manage user parents.
 */
public sealed interface UserParent permits UserParentProvider {
    /**
     * Checks if a parent exists.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return True if the parent exists, otherwise false.
     */
    boolean existsParent(int userId, int parentId);

    /**
     * Adds a parent to a user.
     *
     * @param userId    The id of the user.
     * @param creatorId The id of the creator.
     * @param parentId  The id of the parent.
     * @param time      The time the parent was added.
     */
    void addParent(int userId, int creatorId, int parentId, long time);

    /**
     * Removes a parent from a user.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     */
    void removeParent(int userId, int parentId);

    /**
     * Clears all parents from a user.
     *
     * @param userId The id of the user.
     */
    void clearParent(int userId);

    /**
     * Obtains the parent id of a user.
     *
     * @param userId The id of the user.
     * @return The parent id of the user.
     */
    int getParentId(int userId);

    /**
     * Obtains all parent ids of a user.
     *
     * @param userId The id of the user.
     * @return A list of all parent ids of the user.
     */
    List<Integer> getParentIds(int userId);

    /**
     * Obtains all parent names of a user.
     *
     * @param group  The group.
     * @param userId The id of the user.
     * @return A list of all parent names of the user.
     */
    List<String> getParentNames(Group group, int userId);

    /**
     * Obtains the creator id of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The creator id of the parent.
     */
    int getCreatorId(int userId, int parentId);

    /**
     * Obtains the created time of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The created time of the parent.
     */
    long getCreatedTime(int userId, int parentId);

    /**
     * Obtains the created date of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The created date of the parent.
     */
    String getCreatedDate(int userId, int parentId);

    /**
     * Obtains the expired time of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The expired time of the parent.
     */
    long getExpiredTime(int userId, int parentId);

    /**
     * Obtains the expired date of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The expired date of the parent.
     */
    String getExpiredDate(int userId, int parentId);

    /**
     * Sets the expired time of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @param time     The time the parent expires.
     * @return The date the parent will expire.
     */
    String setExpiredTime(int userId, int parentId, long time);

    /**
     * Adds an expired time to a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @param time     The time to add to the expired time.
     * @return The date the parent will expire.
     */
    String addExpiredTime(int userId, int parentId, long time);

    /**
     * Removes an expired time from a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @param time     The time to remove from the expired time.
     * @return The date the parent will expire.
     */
    String removeExpiredTime(int userId, int parentId, long time);

    /**
     * Loads all expired parents of a user.
     *
     * @param user The user.
     */
    void loadExpired(User user);
}
