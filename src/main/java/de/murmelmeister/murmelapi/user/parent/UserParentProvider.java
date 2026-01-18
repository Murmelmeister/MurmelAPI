package de.murmelmeister.murmelapi.user.parent;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a user-parent relationship in the MurmelAPI.
 * <p>
 * This interface is used to define the operations that can be performed on user-parent relationships.
 * It is a marker interface and does not contain any methods.
 * </p>
 * <p>
 * The {@link UserParentProviderImpl} interface extends this interface to provide additional functionality.
 * </p>
 */
public sealed interface UserParentProvider permits UserParentProviderImpl {
    void refreshCache();

    @Nullable UserParent getParent(int userId, int parentId);

    @Nullable List<UserParent> getParents(int userId);

    @Nullable UserParent add(int userId, int parentId, long duration, int createdBy);

    int remove(int userId, int parentId);

    int clear(int userId);

    @Nullable UserParent update(int userId, int parentId, long duration, int changedBy);

    int loadExpired();
}
