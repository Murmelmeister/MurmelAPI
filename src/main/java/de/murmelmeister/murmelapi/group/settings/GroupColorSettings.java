package de.murmelmeister.murmelapi.group.settings;

/**
 * Group color settings interface to manage group color settings.
 */
public sealed interface GroupColorSettings permits GroupColorSettingsProvider {
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
     */
    void createGroup(int groupId, int creatorId);

    /**
     * Creates a new group and checks if the group already exists.
     * If the group already exists, the method will return without creating a new group.
     *
     * @param groupId    The id of the group.
     * @param creatorId  The creator id of the group.
     * @param chatPrefix The chat prefix of the group.
     * @param chatSuffix The chat suffix of the group.
     * @param chatColor  The chat color of the group.
     * @param tabPrefix  The tab prefix of the group.
     * @param tabSuffix  The tab suffix of the group.
     * @param tabColor   The tab color of the group.
     * @param tagPrefix  The tag prefix of the group.
     * @param tagSuffix  The tag suffix of the group.
     * @param tagColor   The tag color of the group.
     */
    void createGroup(int groupId, int creatorId, String chatPrefix, String chatSuffix, String chatColor, String tabPrefix, String tabSuffix, String tabColor, String tagPrefix, String tagSuffix, String tagColor);

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
     * Obtains the edited time of a group.
     *
     * @param groupId The id of the group.
     * @return The edited time of the group.
     */
    long getEditedTime(int groupId);

    /**
     * Obtains the edited date of a group.
     *
     * @param groupId The id of the group.
     * @return The edited date of the group.
     */
    String getEditedDate(int groupId);

    /**
     * Obtains the prefix of a group.
     *
     * @param type    The type of the group color.
     * @param groupId The id of the group.
     * @return The prefix of the group.
     */
    String getPrefix(GroupColorType type, int groupId);

    /**
     * Obtains the suffix of a group.
     *
     * @param type    The type of the group color.
     * @param groupId The id of the group.
     * @return The suffix of the group.
     */
    String getSuffix(GroupColorType type, int groupId);

    /**
     * Obtains the color of a group.
     *
     * @param type    The type of the group color.
     * @param groupId The id of the group.
     * @return The color of the group.
     */
    String getColor(GroupColorType type, int groupId);

    /**
     * Sets the prefix of a group.
     *
     * @param type      The type of the group color.
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param prefix    The prefix of the group.
     */
    void setPrefix(GroupColorType type, int groupId, int creatorId, String prefix);

    /**
     * Sets the suffix of a group.
     *
     * @param type      The type of the group color.
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param suffix    The suffix of the group.
     */
    void setSuffix(GroupColorType type, int groupId, int creatorId, String suffix);

    /**
     * Sets the color of a group.
     *
     * @param type      The type of the group color.
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param color     The color of the group.
     */
    void setColor(GroupColorType type, int groupId, int creatorId, String color);
}
