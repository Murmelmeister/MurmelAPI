package de.murmelmeister.murmelapi.group.color;

import java.util.List;

/**
 * Represents a color associated with a group in the MurmelAPI.
 * <p>
 * This interface is used to define a color that can be managed by the MurmelAPI.
 * It is a marker interface and does not contain any methods.
 * </p>
 * <p>
 * The {@link GroupColorProviderImpl} interface extends this interface to provide additional functionality.
 * </p>
 */
public interface GroupColorProvider {
    void closeCache();

    void refreshCache();

    GroupColor getGroupColor(int groupId, int typeId);

    List<GroupColor> getGroupColors(int groupId);

    List<GroupColor> getGroupColors();

    GroupColor add(int groupId, int typeId, String value, int createdBy);

    int remove(int groupId, int typeId);

    int clear(int groupId);

    GroupColor update(int groupId, int typeId, String value, int changedBy);
}
