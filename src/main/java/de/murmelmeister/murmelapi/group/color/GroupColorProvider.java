package de.murmelmeister.murmelapi.group.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

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
    void refreshCache();

    @Nullable GroupColor findGroupColor(int groupId, int typeId);

    @NotNull
    @Unmodifiable
    List<GroupColor> findGroupColors(int groupId);

    @NotNull
    @Unmodifiable
    List<GroupColor> findGroupColors();

    @Nullable GroupColor add(int groupId, int typeId, @NotNull String value, int createdBy);

    int remove(int groupId, int typeId);

    int clear(int groupId);

    @Nullable GroupColor update(int groupId, int typeId, @NotNull String value, int changedBy);

    @Nullable GroupColor upsert(int groupId, int typeId, @NotNull String value, int executorId);

    @Nullable GroupColor upsert(@NotNull GroupColor groupColor, int executorId);
}
