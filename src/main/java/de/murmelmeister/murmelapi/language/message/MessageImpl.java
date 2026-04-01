package de.murmelmeister.murmelapi.language.message;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

record MessageImpl(
        int id,
        @NotNull String tagId,
        int languageId,
        @NotNull String message
) implements Message {

    public MessageImpl {
        Objects.requireNonNull(tagId, "tagId must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (tagId.length() > 255) throw new IllegalArgumentException("tagId cannot be longer than 255 characters");
    }

    public @NotNull Message.Builder builder() {
        return new Builder(this);
    }

    public @NotNull Message with(@NotNull Consumer<Message.Builder> consumer) {
        Message.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements Message.Builder {
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

        public @NotNull Message.Builder tagId(@NotNull String tagId) {
            this.tagId = tagId;
            return this;
        }

        public @NotNull Message.Builder languageId(int languageId) {
            this.languageId = languageId;
            return this;
        }

        public @NotNull Message.Builder message(@NotNull String message) {
            this.message = message;
            return this;
        }

        public @NotNull Message build() {
            return new MessageImpl(id, tagId, languageId, message);
        }
    }
}
