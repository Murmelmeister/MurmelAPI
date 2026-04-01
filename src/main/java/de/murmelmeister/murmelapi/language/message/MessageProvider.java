package de.murmelmeister.murmelapi.language.message;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MessageProvider {
    void refreshCache();

    @NotNull Optional<Message> findMessage(int messageId);

    @NotNull Optional<Message> findMessage(@NotNull String tagId, int languageId);

    @NotNull
    @Unmodifiable
    List<Message> findMessages(int languageId);

    @NotNull Optional<Message> upsert(@NotNull String tagId, int languageId, @NotNull String message);

    int delete(@NotNull String tagId, int languageId);

    int delete(int languageId);

    int @NotNull [] upsertAll(@NotNull Collection<Message> messages);

    @ApiStatus.Internal
    static @NotNull MessageProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new MessageProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
