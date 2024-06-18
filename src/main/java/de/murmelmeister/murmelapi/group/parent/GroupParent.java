package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.group.Group;

import java.util.List;

/**
 * Group parent interface to manage group parents.
 */
public sealed interface GroupParent permits GroupParentProvider {
    /**
     * Checks if a parent exists.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return True if the parent exists, otherwise false.
     */
    boolean existsParent(int groupId, int parentId);

    /**
     * Adds a parent to a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The id of the creator.
     * @param parentId  The id of the parent.
     * @param time      The time the parent was added.
     */
    void addParent(int groupId, int creatorId, int parentId, long time);

    /**
     * Removes a parent from a group.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     */
    void removeParent(int groupId, int parentId);

    /**
     * Clears all parents from a group.
     *
     * @param groupId The id of the group.
     */
    void clearParent(int groupId);

    /**
     * Obtains all parent ids of a group.
     *
     * @param groupId The id of the group.
     * @return A list of all parent ids of the group.
     */
    List<Integer> getParentIds(int groupId);

    /**
     * Obtains all parent names of a group.
     *
     * @param group   The group.
     * @param groupId The id of the group.
     * @return A list of all parent names of the group.
     */
    List<String> getParentNames(Group group, int groupId);

    /**
     * Obtains the creator id of a parent.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The creator id of the parent.
     */
    int getCreatorId(int groupId, int parentId);

    /**
     * Obtains the time the parent was created.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The time the parent was created.
     */
    long getCreatedTime(int groupId, int parentId);

    /**
     * Obtains the date the parent was created.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The date the parent was created.
     */
    String getCreatedDate(int groupId, int parentId);

    /**
     * Obtains the time the parent will expire.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The time the parent will expire.
     */
    long getExpiredTime(int groupId, int parentId);

    /**
     * Obtains the date the parent will expire.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The date the parent will expire.
     */
    String getExpiredDate(int groupId, int parentId);

    /**
     * Sets the time the parent will expire.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @param time     The time the parent will expire.
     * @return The date the parent will expire.
     */
    String setExpiredTime(int groupId, int parentId, long time);

    /**
     * Adds time to the parent expiration.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @param time     The time to add to the expiration.
     * @return The date the parent will expire.
     */
    String addExpiredTime(int groupId, int parentId, long time);

    /**
     * Removes time from the parent expiration.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @param time     The time to remove from the expiration.
     * @return The date the parent will expire.
     */
    String removeExpiredTime(int groupId, int parentId, long time);

    /**
     * Loads all expired parents of a group.
     *
     * @param group The group.
     */
    void loadExpired(Group group);
}
