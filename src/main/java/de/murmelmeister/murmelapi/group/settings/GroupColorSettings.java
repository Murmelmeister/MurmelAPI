package de.murmelmeister.murmelapi.group.settings;

import java.sql.SQLException;

/**
 * Group color settings interface to manage group color settings.
 */
public sealed interface GroupColorSettings permits GroupColorSettingsProvider {
    /**
     * Checks if a group exists.
     *
     * @param groupId The id of the group.
     * @return True if the group exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsGroup(int groupId) throws SQLException;

    /**
     * Creates a new group and checks if the group already exists.
     * If the group already exists, the method will return without creating a new group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void createGroup(int groupId, int creatorId) throws SQLException;

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
     * @throws SQLException If an SQL error occurs.
     */
    void createGroup(int groupId, int creatorId, String chatPrefix, String chatSuffix, String chatColor, String tabPrefix, String tabSuffix, String tabColor, String tagPrefix, String tagSuffix, String tagColor) throws SQLException;

    /**
     * Deletes a group.
     *
     * @param groupId The id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void deleteGroup(int groupId) throws SQLException;

    /**
     * Obtains the creator id of a group.
     *
     * @param groupId The id of the group.
     * @return The creator id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    int getCreatorId(int groupId) throws SQLException;

    /**
     * Obtains the edited time of a group.
     *
     * @param groupId The id of the group.
     * @return The edited time of the group.
     * @throws SQLException If an SQL error occurs.
     */
    long getEditedTime(int groupId) throws SQLException;

    /**
     * Obtains the edited date of a group.
     *
     * @param groupId The id of the group.
     * @return The edited date of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getEditedDate(int groupId) throws SQLException;

    /**
     * Obtains the chat prefix of a group.
     *
     * @param groupId The id of the group.
     * @return The chat prefix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getChatPrefix(int groupId) throws SQLException;

    /**
     * Sets the chat prefix of a group.
     *
     * @param groupId    The id of the group.
     * @param creatorId  The creator id of the group.
     * @param chatPrefix The chat prefix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setChatPrefix(int groupId, int creatorId, String chatPrefix) throws SQLException;

    /**
     * Obtains the chat suffix of a group.
     *
     * @param groupId The id of the group.
     * @return The chat suffix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getChatSuffix(int groupId) throws SQLException;

    /**
     * Sets the chat suffix of a group.
     *
     * @param groupId    The id of the group.
     * @param creatorId  The creator id of the group.
     * @param chatSuffix The chat suffix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setChatSuffix(int groupId, int creatorId, String chatSuffix) throws SQLException;

    /**
     * Obtains the chat color of a group.
     *
     * @param groupId The id of the group.
     * @return The chat color of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getChatColor(int groupId) throws SQLException;

    /**
     * Sets the chat color of a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param chatColor The chat color of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setChatColor(int groupId, int creatorId, String chatColor) throws SQLException;

    /**
     * Obtains the tab prefix of a group.
     *
     * @param groupId The id of the group.
     * @return The tab prefix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getTabPrefix(int groupId) throws SQLException;

    /**
     * Sets the tab prefix of a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param tabPrefix The tab prefix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setTabPrefix(int groupId, int creatorId, String tabPrefix) throws SQLException;

    /**
     * Obtains the tab suffix of a group.
     *
     * @param groupId The id of the group.
     * @return The tab suffix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getTabSuffix(int groupId) throws SQLException;

    /**
     * Sets the tab suffix of a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param tabSuffix The tab suffix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setTabSuffix(int groupId, int creatorId, String tabSuffix) throws SQLException;

    /**
     * Obtains the tab color of a group.
     *
     * @param groupId The id of the group.
     * @return The tab color of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getTabColor(int groupId) throws SQLException;

    /**
     * Sets the tab color of a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param tabColor  The tab color of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setTabColor(int groupId, int creatorId, String tabColor) throws SQLException;

    /**
     * Obtains the tag prefix of a group.
     *
     * @param groupId The id of the group.
     * @return The tag prefix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getTagPrefix(int groupId) throws SQLException;

    /**
     * Sets the tag prefix of a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param tagPrefix The tag prefix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setTagPrefix(int groupId, int creatorId, String tagPrefix) throws SQLException;

    /**
     * Obtains the tag suffix of a group.
     *
     * @param groupId The id of the group.
     * @return The tag suffix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getTagSuffix(int groupId) throws SQLException;

    /**
     * Sets the tag suffix of a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param tagSuffix The tag suffix of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setTagSuffix(int groupId, int creatorId, String tagSuffix) throws SQLException;

    /**
     * Obtains the tag color of a group.
     *
     * @param groupId The id of the group.
     * @return The tag color of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getTagColor(int groupId) throws SQLException;

    /**
     * Sets the tag color of a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param tagColor  The tag color of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setTagColor(int groupId, int creatorId, String tagColor) throws SQLException;
}
