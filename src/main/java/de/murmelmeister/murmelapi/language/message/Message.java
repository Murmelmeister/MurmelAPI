package de.murmelmeister.murmelapi.language.message;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a message in the Murmel API.
 * Each message has an ID, a tag, a language ID, and the actual message content.
 */
public record Message(
        int id,
        @NotNull String tagId,
        int languageId,
        @NotNull String message
) {
    public Message {
        Objects.requireNonNull(tagId, "tagId must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    public static @NotNull Builder builder(@NotNull Message message) {
        return new Builder(message);
    }

    public static class Builder {
        private final int id;
        private String tagId;
        private int languageId;
        private String message;

        private Builder(@NotNull Message message) {
            this.id = message.id();
            this.tagId = message.tagId();
            this.languageId = message.languageId();
            this.message = message.message();
        }

        public Builder tagId(@NotNull String tagId) {
            this.tagId = tagId;
            return this;
        }

        public Builder languageId(int languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder message(@NotNull String message) {
            this.message = message;
            return this;
        }

        public @NotNull Message build() {
            return new Message(id, tagId, languageId, message);
        }
    }
}
