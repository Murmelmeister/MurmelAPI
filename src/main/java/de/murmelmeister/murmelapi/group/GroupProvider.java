package de.murmelmeister.murmelapi.group;

import java.util.List;

/**
 * Represents a group in the MurmelAPI.
 * <p>
 * This interface is used to define a group that can be managed by the MurmelAPI.
 * It is a marker interface and does not contain any methods.
 * </p>
 * <p>
 * The {@link GroupProviderImpl} interface extends this interface to provide additional functionality.
 * </p>
 */
public sealed interface GroupProvider permits GroupProviderImpl {
    void closeCache();

    void refreshCache();

    Group findById(int id);

    Group findByName(String groupName);

    List<Group> findAll();

    List<String> findAllGroupNames();

    Group create(String groupName, int priority, int createdBy);

    int delete(int groupId);

    Group update(int groupId, String groupName, int priority, int changedBy);
}
