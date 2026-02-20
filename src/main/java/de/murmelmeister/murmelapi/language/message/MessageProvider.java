package de.murmelmeister.murmelapi.language.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

public interface MessageProvider {
    void refreshCache();

    @Nullable Message get(int messageId);

    @Nullable Message get(@NotNull String tagId, int languageId);

    @NotNull @Unmodifiable
    List<Message> getAllMessages(int languageId);

    @Nullable Message create(@NotNull String tagId, int languageId, @NotNull String message);

    int delete(int id);

    int delete(@NotNull String tagId, int languageId);

    int deleteAll(int languageId);

    @Nullable Message update(int id, @NotNull String tagId, int languageId, @NotNull String message);

    /**
     * Upserts all entries from a single properties file.
     */
    int @NotNull [] upsertAll(@NotNull Properties properties);

    /**
     * Upserts all entries from multiple properties files (e.g., multiple languages).
     */
    int @NotNull [] upsertAll(@NotNull Collection<Properties> properties);

    /**
     * Backwards-compatible alias.
     */
    default int @NotNull [] createOrUpdateAll(@NotNull Properties properties) {
        return upsertAll(properties);
    }
}
