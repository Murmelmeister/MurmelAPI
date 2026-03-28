package de.murmelmeister.murmelapi.group;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

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

    @NotNull Optional<Group> findById(int id);

    @NotNull Optional<Group> findByName(@NotNull String groupName);

    @NotNull @Unmodifiable List<Group> findAll();

    @NotNull @Unmodifiable List<String> findAllGroupNames();

    @NotNull Optional<Group> create(@NotNull String groupName, int priority, int createdBy);

    int delete(int groupId);

    @NotNull Optional<Group> update(int groupId, @NotNull String groupName, int priority, int changedBy);

    @ApiStatus.Internal
    static @NotNull GroupProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new GroupProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
