package de.murmelmeister.murmelapi.group;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    void refreshCache();

    @Nullable Group findById(int id);

    @Nullable Group findByName(@Nullable String groupName);

    @NotNull List<Group> findAll();

    @NotNull List<String> findAllGroupNames();

    @Nullable Group create(@NotNull String groupName, int priority, int createdBy);

    int delete(int groupId);

    @Nullable Group update(int groupId, @NotNull String groupName, int priority, int changedBy);
}
