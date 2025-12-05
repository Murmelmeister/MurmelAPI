package de.murmelmeister.murmelapi.group.parent;

import java.util.List;

/**
 * GroupParent is an interface that provides methods to manage group parents in the database.
 * It defines methods for checking existence, adding, removing, and retrieving parent groups.
 * It also provides methods for managing expiration dates and user information related to group parents.
 */
public sealed interface GroupParentProvider permits GroupParentProviderImpl {
    void refreshCache();

    GroupParent getParent(int groupId, int parentId);

    List<GroupParent> getParents(int groupId);

    GroupParent add(int groupId, int parentId, long duration, int createdBy);

    int remove(int groupId, int parentId);

    int clear(int groupId);

    GroupParent update(int groupId, int parentId, long duration, int changedBy);

    int loadExpired();
}
