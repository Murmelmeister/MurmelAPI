package de.murmelmeister.murmelapi.group.settings;

/**
 * Group settings interface to manage group settings.
 */
public sealed interface GroupSettings permits GroupSettingsProvider {
    /**
     * Checks if a group exists.
     *
     * @param groupId The id of the group.
     * @return True if the group exists, otherwise false.
     */
    boolean existsGroup(int groupId);

    /**
     * Creates a new group and checks if the group already exists.
     * If the group already exists, the method will return without creating a new group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param sortId    The sort id of the group.
     * @param teamId    The team id of the group.
     */
    void createGroup(int groupId, int creatorId, int sortId, String teamId);

    /**
     * Deletes a group.
     *
     * @param groupId The id of the group.
     */
    void deleteGroup(int groupId);

    /**
     * Obtains the creator id of a group.
     *
     * @param groupId The id of the group.
     * @return The creator id of the group.
     */
    int getCreatorId(int groupId);

    /**
     * Obtains the created time of a group.
     *
     * @param groupId The id of the group.
     * @return The created time of the group.
     */
    long getCreatedTime(int groupId);

    /**
     * Obtains the created date of a group.
     *
     * @param groupId The id of the group.
     * @return The created date of the group.
     */
    String getCreatedDate(int groupId);

    /**
     * Obtains the sort id of a group.
     *
     * @param groupId The id of the group.
     * @return The sort id of the group.
     */
    int getSortId(int groupId);

    /**
     * Sets the sort id of a group.
     *
     * @param groupId The id of the group.
     * @param sortId  The sort id of the group.
     */
    void setSortId(int groupId, int sortId);

    /**
     * Obtains the team id of a group.
     *
     * @param groupId The id of the group.
     * @return The team id of the group.
     */
    String getTeamId(int groupId);

    /**
     * Sets the team id of a group.
     *
     * @param groupId The id of the group.
     * @param teamId  The team id of the group.
     */
    void setTeamId(int groupId, String teamId);
}
