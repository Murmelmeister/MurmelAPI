package de.murmelmeister.murmelapi.group.color;

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
    void refreshCache();

    @NotNull Optional<GroupColor> findGroupColor(int groupId, int typeId);

    @NotNull
    @Unmodifiable
    List<GroupColor> findGroupColors(int groupId);

    @NotNull
    @Unmodifiable
    List<GroupColor> findGroupColors();

    @NotNull Optional<GroupColor> upsert(int groupId, int typeId, @NotNull String value, int executorId);

    @NotNull Optional<GroupColor> upsert(@NotNull GroupColor groupColor, int executorId);

    int remove(int groupId, int typeId);

    int clear(int groupId);

    @ApiStatus.Internal
    static @NotNull GroupColorProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new GroupColorProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
