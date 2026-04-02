package de.murmelmeister.murmelapi.settings;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

record SettingsImpl(
        @NotNull String tagId,
        @NotNull String json,
        @NotNull LocalDateTime updatedAt
) implements Settings {

    public SettingsImpl {
        Objects.requireNonNull(tagId, "tagId must not be null");
        Objects.requireNonNull(json, "json must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (tagId.length() > 100) throw new IllegalArgumentException("tagId cannot be longer than 100 characters");
    }

    public @NotNull Settings.Builder builder() {
        return new Builder(this);
    }

    public @NotNull Settings with(@NotNull Consumer<Settings.Builder> consumer) {
        Settings.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements Settings.Builder {
        private final String tagId;
        private String json;
        private LocalDateTime updatedAt;

        private Builder(@NotNull Settings settings) {
            this.tagId = settings.tagId();
            this.json = settings.json();
            this.updatedAt = settings.updatedAt();
        }

        public @NotNull Settings.Builder json(@NotNull String json) {
            this.json = json;
            return this;
        }

        public @NotNull Settings.Builder updatedAt(@NotNull LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public @NotNull Settings build() {
            return new SettingsImpl(tagId, json, updatedAt);
        }
    }
}
