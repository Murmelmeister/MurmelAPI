package de.murmelmeister.murmelapi.language.message;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Represents a message in the Murmel API.
 * Each message has an ID, a tag, a language ID, and the actual message content.
 */
public interface Message {
    int id();

    @NotNull String tagId();

    int languageId();

    @NotNull String message();

    @NotNull Builder builder();

    @NotNull Message with(@NotNull Consumer<Builder> consumer);

    static @NotNull Message of(int id, @NotNull String tagId, int languageId, @NotNull String message) {
        return new MessageImpl(id, tagId, languageId, message);
    }

    interface Builder {
        @NotNull Builder tagId(@NotNull String tagId);

        @NotNull Builder languageId(int languageId);

        @NotNull Builder message(@NotNull String message);

        @NotNull Message build();
    }
}
