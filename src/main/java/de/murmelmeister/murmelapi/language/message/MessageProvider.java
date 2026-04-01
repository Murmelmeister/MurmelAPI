package de.murmelmeister.murmelapi.language.message;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public interface MessageProvider {
    void refreshCache();

    @NotNull Optional<Message> get(int messageId);

    @NotNull Optional<Message> get(@NotNull String tagId, int languageId);

    @NotNull
    @Unmodifiable
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

    @ApiStatus.Internal
    static @NotNull MessageProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new MessageProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
